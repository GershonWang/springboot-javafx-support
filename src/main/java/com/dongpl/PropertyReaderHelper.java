package com.dongpl;

import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PropertyReaderHelper {

    private PropertyReaderHelper() {
    }

    public static List<String> get(final Environment env, final String propName) {
        final List<String> list = new ArrayList<>();
        final String singleProp = env.getProperty(propName);
        if (singleProp != null) {
            list.add(singleProp);
            return list;
        }
        int counter = 0;
        String prop = env.getProperty(propName + "[" + counter + "]");
        while (prop != null) {
            list.add(prop);
            counter++;
            prop = env.getProperty(propName + "[" + counter + "]");
        }
        return list;
    }

    public static <T> void setIfPresent(final Environment env, final String key, final Class<T> type, final Consumer<T> function) {
        final T value = env.getProperty(key, type);
        if (value != null) {
            function.accept(value);
        }
    }

    public static String determineFilePathFromPackageName(final Class<?> clazz) {
        return "/" + clazz.getPackage().getName().replace('.', '/') + "/";
    }
}
