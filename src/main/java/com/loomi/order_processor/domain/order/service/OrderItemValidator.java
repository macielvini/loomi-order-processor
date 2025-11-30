package com.loomi.order_processor.domain.order.service;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.product.dto.ProductType;

public interface OrderItemValidator {

    boolean supports(ProductType type);

    ItemHandlerResult validate(OrderItem item);
    
}
