package com.loomi.order_processor.infra.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.loomi.order_processor.domain.exception.HttpException;

@RestControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<?> handleHttpExceptions(HttpException e) {
        return ResponseEntity.status(e.status().value()).body(e.toJson());
    }
    
}
