package com.dongpl;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    public static URL getResource(String path) {
        String decodePath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        // 路径前带 / ，定位到classpath，否则定位当前类的同级
        return ResourceUtil.class.getResource(decodePath);
    }

    public static URL getResourceByClassLoader(ClassLoader classLoader, String path) {
        String decodePath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        // 路径前不能有 / ，且永远定位到classpath
        return classLoader.getResource(decodePath);
    }

}
