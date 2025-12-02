package com.loomi.order_processor.app.service.order.handler.global;

import com.loomi.order_processor.domain.order.usecase.IsOrderValid;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

@Service
public class OrderIsPendingHandler implements OrderHandler, IsOrderValid {
    
    @Override
    public ValidationResult validate(Order order) {
        return this.isNotPending(order);
    }

    @Override
    public OrderProcessResult process(Order order) {
        var isNotPendingValidation = this.isNotPending(order);

        if (!isNotPendingValidation.isValid()) {
            return OrderProcessResult.fail(OrderError.INTERNAL_ERROR.toString());
        }

        return OrderProcessResult.ok();
    }

    @Override
    public ValidationResult isNotPending(Order order) {
        if (!order.status().equals(OrderStatus.PENDING)) {
            return ValidationResult.fail("Order is not pending");
        }
        return ValidationResult.ok();
    }
}
