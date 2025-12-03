package com.loomi.order.app.service.order.handler;

import com.loomi.order.domain.order.dto.OrderProcessResult;
import com.loomi.order.domain.order.entity.Order;
import com.loomi.order.domain.product.dto.ValidationResult;

public interface OrderHandler {
    public ValidationResult validate(Order order);

    public OrderProcessResult process(Order order);
}
