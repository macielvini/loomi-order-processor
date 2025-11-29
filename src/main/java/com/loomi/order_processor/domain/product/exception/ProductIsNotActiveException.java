package com.loomi.order_processor.domain.product.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.loomi.order_processor.domain.exception.HttpException;

public class ProductIsNotActiveException extends HttpException {
    
    public ProductIsNotActiveException(UUID productId) {
        super(HttpStatus.BAD_REQUEST, "Product " + productId + " is not available");
    }
}
