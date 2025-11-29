package com.loomi.order_processor.domain.product.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.loomi.order_processor.domain.product.dto.ProductType;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidationStrategyFor {
    ProductType[] value();
}
