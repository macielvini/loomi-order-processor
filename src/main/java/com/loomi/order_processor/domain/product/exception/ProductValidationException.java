package com.loomi.order_processor.domain.product.exception;

import org.springframework.http.HttpStatus;

import com.loomi.order_processor.domain.exception.HttpException;

public class ProductValidationException extends HttpException {
    public ProductValidationException(String results) {
        super(HttpStatus.BAD_REQUEST, results);
    }

}
