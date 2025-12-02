package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

public interface OrderHandler {
    public ValidationResult validate(Order order);

    public OrderProcessResult process(Order order);
}
