package com.loomi.order.domain.product.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.loomi.order.domain.exception.HttpException;

public class ProductIsNotActiveException extends HttpException {
    
    public ProductIsNotActiveException(UUID productId) {
        super(HttpStatus.BAD_REQUEST, "Product " + productId + " is not available");
    }
}
