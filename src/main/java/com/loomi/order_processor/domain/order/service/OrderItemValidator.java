package com.loomi.order_processor.domain.order.service;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderItemValidationResult;
import com.loomi.order_processor.domain.product.dto.ProductType;

public interface OrderItemValidator {

    boolean supports(ProductType type);

    OrderItemValidationResult validate(OrderItem item);
    
}
