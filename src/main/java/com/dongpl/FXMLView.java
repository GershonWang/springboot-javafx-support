package com.dongpl;

import org.springframework.stereotype.Component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Component
@Retention(RetentionPolicy.RUNTIME)
public @interface FXMLView {

	String value() default "";

	String[] css() default {};

	String bundle() default "";

	String encoding() default "ISO-8859-1";
	
	String title() default "";
	
	String stageStyle() default "UTILITY";
}
