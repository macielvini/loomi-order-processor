package com.loomi.order.app.service.order.handler;

import org.springframework.stereotype.Service;

import com.loomi.order.domain.order.dto.OrderProcessResult;
import com.loomi.order.domain.order.entity.Order;
import com.loomi.order.domain.order.valueobject.OrderStatus;
import com.loomi.order.domain.product.dto.ValidationResult;

@Service
public class OrderIsPendingHandler implements OrderHandler {
    
    @Override
    public ValidationResult validate(Order order) {
        if (order.status() != OrderStatus.PENDING) {
            return ValidationResult.fail("Order is not pending");
        }
        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(Order order) {
        return OrderProcessResult.ok();
    }
    
}
