package com.dongpl;

import javafx.scene.Parent;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class SplashScreen {

	private static final String DEFAULT_IMAGE = "splash/javafx.png";

	public Parent getParent() {
		final ImageView imageView = new ImageView(ResourceUtil.getResource(getImagePath()).toExternalForm());
		final ProgressBar splashProgressBar = new ProgressBar();
		splashProgressBar.setPrefWidth(imageView.getImage().getWidth());
		final VBox vbox = new VBox();
		vbox.getChildren().addAll(imageView, splashProgressBar);
		return vbox;
	}

	public boolean visible() {
		return true;
	}

	public String getImagePath() {
		return DEFAULT_IMAGE;
	}

}
