package com.myngshj3.testrunner.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.interceptor.annotation.Interceptor;

@Inherited
@Interceptor
@Retention(RetentionPolicy.RUNTIME)
public @interface DataModelCheck {
	
}