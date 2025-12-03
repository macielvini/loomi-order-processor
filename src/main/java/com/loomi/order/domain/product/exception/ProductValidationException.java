package com.loomi.order.domain.product.exception;

import org.springframework.http.HttpStatus;

import com.loomi.order.domain.exception.HttpException;

public class ProductValidationException extends HttpException {
    public ProductValidationException(String results) {
        super(HttpStatus.BAD_REQUEST, results);
    }

}
