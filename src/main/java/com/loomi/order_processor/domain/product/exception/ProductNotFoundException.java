package com.loomi.order_processor.domain.product.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.loomi.order_processor.domain.exception.HttpException;

public class ProductNotFoundException extends HttpException {

    public ProductNotFoundException(UUID productId) {
        super(HttpStatus.NOT_FOUND,"Product " + productId + " not found");
    }

    public ProductNotFoundException(String productId) {
        super(HttpStatus.NOT_FOUND,"Product " + productId + " not found");
    }
}
