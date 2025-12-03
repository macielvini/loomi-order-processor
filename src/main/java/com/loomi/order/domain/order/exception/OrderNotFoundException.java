package com.loomi.order.domain.order.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.loomi.order.domain.exception.HttpException;

public class OrderNotFoundException extends HttpException {
    
    public OrderNotFoundException(UUID orderId) {
        super(HttpStatus.NOT_FOUND, "Order not found with id: " + orderId);
    }
}
