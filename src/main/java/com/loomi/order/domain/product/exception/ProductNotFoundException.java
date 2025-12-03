package com.loomi.order.domain.product.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.loomi.order.domain.exception.HttpException;

public class ProductNotFoundException extends HttpException {

    public ProductNotFoundException(UUID productId) {
        super(HttpStatus.NOT_FOUND,"Product " + productId + " not found");
    }

    public ProductNotFoundException(String productId) {
        super(HttpStatus.NOT_FOUND,"Product " + productId + " not found");
    }
}
