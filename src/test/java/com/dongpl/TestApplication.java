package com.dongpl;

import com.dongpl.views.DemoSceneView;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest
@ComponentScan("com.dongpl")
public class TestApplication {

    public static void main(String[] args) {
        AbstractJavaFxApplicationSupport.launch(TestApplication.class, MainView.class, DemoSceneView.class, args);
    }

}
