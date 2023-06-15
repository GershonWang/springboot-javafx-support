package com.dongpl;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.ResourceBundle.getBundle;

public abstract class AbstractFxmlView implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFxmlView.class);

    private final ObjectProperty<Object> presenterProperty;

    private final Optional<ResourceBundle> bundle;

    private final URL resource;

    private final FXMLView annotation;

    private FXMLLoader fxmlLoader;

    private ApplicationContext applicationContext;

    private String fxmlRoot;

    private Stage stage;

    private Modality currentStageModality;

    private boolean isPrimaryStageView = false;

    public AbstractFxmlView() {
        LOGGER.debug("AbstractFxmlView construction");
        final String filePathFromPackageName = PropertyReaderHelper.determineFilePathFromPackageName(getClass());
        setFxmlRootPath(filePathFromPackageName);
        annotation = getFXMLAnnotation();
        resource = getURLResource(annotation);
        presenterProperty = new SimpleObjectProperty<>();
        bundle = getResourceBundle(getBundleName());
    }

    private static String stripEnding(final String clazz) {
        if (!clazz.endsWith("view")) {
            return clazz;
        }
        return clazz.substring(0, clazz.lastIndexOf("view"));
    }

    private URL getURLResource(final FXMLView annotation) {
        if (annotation != null && !annotation.value().equals("")) {
            return getClass().getResource(annotation.value());
        } else {
            return getClass().getResource(getFxmlPath());
        }
    }

    private FXMLView getFXMLAnnotation() {
        final Class<? extends AbstractFxmlView> theClass = this.getClass();
        return theClass.getAnnotation(FXMLView.class);
    }

    private Object createControllerForType(final Class<?> type) {
        return applicationContext.getBean(type);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        if (this.applicationContext != null) {
            return;
        }
        this.applicationContext = applicationContext;
    }

    private void setFxmlRootPath(final String path) {
        fxmlRoot = path;
    }

    private FXMLLoader loadSynchronously(final URL resource, final Optional<ResourceBundle> bundle) throws IllegalStateException {
        final FXMLLoader loader = new FXMLLoader(resource, bundle.orElse(null));
        loader.setControllerFactory(this::createControllerForType);
        try {
            loader.load();
        } catch (final IOException | IllegalStateException e) {
            throw new IllegalStateException("Cannot load " + getConventionalName(), e);
        }
        return loader;
    }

    private void ensureFxmlLoaderInitialized() {
        if (fxmlLoader != null) {
            return;
        }
        fxmlLoader = loadSynchronously(resource, bundle);
        presenterProperty.set(fxmlLoader.getController());
    }

    protected void initFirstView() {
        isPrimaryStageView = true;
        stage = GUIState.getStage();
        Scene scene = getView().getScene() != null ? getView().getScene() : new Scene(getView());
        stage.setScene(scene);
        GUIState.setScene(scene);
    }

    public void hide() {
        if (stage != null) stage.hide();
    }

    public void showView(Window window, Modality modality) {
        if (!isPrimaryStageView && (stage == null || currentStageModality != modality || !Objects.equals(stage.getOwner(), window))) {
            stage = createStage(modality);
            stage.initOwner(window);
        }
        stage.show();
    }

    public void showView(Modality modality) {
        if (!isPrimaryStageView && (stage == null || currentStageModality != modality)) {
            stage = createStage(modality);
        }
        stage.show();
    }

    public void showViewAndWait(Window window, Modality modality) {
        if (isPrimaryStageView) {
            showView(modality); // this modality will be ignored anyway
            return;
        }
        if (stage == null || currentStageModality != modality || !Objects.equals(stage.getOwner(), window)) {
            stage = createStage(modality);
            stage.initOwner(window);
        }
        stage.showAndWait();
    }

    public void showViewAndWait(Modality modality) {
        if (isPrimaryStageView) {
            showView(modality); // this modality will be ignored anyway
            return;
        }
        if (stage == null || currentStageModality != modality) {
            stage = createStage(modality);
        }
        stage.showAndWait();
    }

    private Stage createStage(Modality modality) {
        currentStageModality = modality;
        Stage stage = new Stage();
        stage.initModality(modality);
        stage.setTitle(getDefaultTitle());
        stage.initStyle(getDefaultStyle());
        List<Image> primaryStageIcons = GUIState.getStage().getIcons();
        stage.getIcons().addAll(primaryStageIcons);
        Scene scene = getView().getScene() != null ? getView().getScene() : new Scene(getView());
        stage.setScene(scene);
        return stage;
    }

    public Parent getView() {
        ensureFxmlLoaderInitialized();
        final Parent parent = fxmlLoader.getRoot();
        addCSSIfAvailable(parent);
        return parent;
    }

    public void getView(final Consumer<Parent> consumer) {
        CompletableFuture.supplyAsync(this::getView, Platform::runLater).thenAccept(consumer);
    }

    public Node getViewWithoutRootContainer() {
        final ObservableList<Node> children = getView().getChildrenUnmodifiable();
        if (children.isEmpty()) {
            return null;
        }
        return children.listIterator().next();
    }

    void addCSSIfAvailable(final Parent parent) {
        final List<String> list = PropertyReaderHelper.get(applicationContext.getEnvironment(), "javafx.css");
        if (!list.isEmpty()) {
            list.forEach(css -> parent.getStylesheets().add(getClass().getResource(css).toExternalForm()));
        }
        addCSSFromAnnotation(parent);
        final URL uri = getClass().getResource(getStyleSheetName());
        if (uri == null) {
            return;
        }
        final String uriToCss = uri.toExternalForm();
        parent.getStylesheets().add(uriToCss);
    }

    private void addCSSFromAnnotation(final Parent parent) {
        if (annotation != null) {
            for (final String cssFile : annotation.css()) {
                final URL uri = getClass().getResource(cssFile);
                if (uri != null) {
                    final String uriToCss = uri.toExternalForm();
                    parent.getStylesheets().add(uriToCss);
                    LOGGER.debug("css file added to parent: {}", cssFile);
                } else {
                    LOGGER.warn("referenced {} css file could not be located", cssFile);
                }
            }
        }
    }

    String getDefaultTitle() {
        return annotation.title();
    }

    StageStyle getDefaultStyle() {
        final String style = annotation.stageStyle();
        return StageStyle.valueOf(style.toUpperCase());
    }

    private String getStyleSheetName() {
        return fxmlRoot + getConventionalName(".css");
    }

    public Object getPresenter() {
        ensureFxmlLoaderInitialized();
        return presenterProperty.get();
    }

    public void getPresenter(final Consumer<Object> presenterConsumer) {

        presenterProperty.addListener((final ObservableValue<? extends Object> o, final Object oldValue, final Object newValue) -> {
            presenterConsumer.accept(newValue);
        });
    }

    private String getConventionalName(final String ending) {
        return getConventionalName() + ending;
    }

    private String getConventionalName() {
        return stripEnding(getClass().getSimpleName().toLowerCase());
    }

    private String getBundleName() {
        if (StringUtils.isEmpty(annotation.bundle())) {
            final String lbundle = getClass().getPackage().getName() + "." + getConventionalName();
            LOGGER.debug("Bundle: {} based on conventional name.", lbundle);
            return lbundle;
        }
        final String lbundle = annotation.bundle();
        LOGGER.debug("Annotated bundle: {}", lbundle);
        return lbundle;
    }

    final String getFxmlPath() {
        final String fxmlPath = fxmlRoot + getConventionalName(".fxml");
        LOGGER.debug("Determined fxmlPath: " + fxmlPath);
        return fxmlPath;
    }

    private Optional<ResourceBundle> getResourceBundle(final String name) {
        try {
            LOGGER.debug("Resource bundle: " + name);
            return Optional.of(getBundle(name, new ResourceBundleControl(getResourceBundleCharset())));
        } catch (final MissingResourceException ex) {
            LOGGER.debug("No resource bundle could be determined: " + ex.getMessage());
            return Optional.empty();
        }
    }

    private Charset getResourceBundleCharset() {
        return Charset.forName(annotation.encoding());
    }

    public Optional<ResourceBundle> getResourceBundle() {
        return bundle;
    }

    @Override
    public String toString() {
        return "AbstractFxmlView [presenterProperty=" + presenterProperty + ", bundle=" + bundle + ", resource=" + resource + ", fxmlRoot=" + fxmlRoot + "]";
    }

}
