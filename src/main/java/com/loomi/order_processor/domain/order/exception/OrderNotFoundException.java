package com.loomi.order_processor.domain.order.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.loomi.order_processor.domain.exception.HttpException;

public class OrderNotFoundException extends HttpException {
    
    public OrderNotFoundException(UUID orderId) {
        super(HttpStatus.NOT_FOUND, "Order not found with id: " + orderId);
    }
}
