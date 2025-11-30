package com.loomi.order_processor.domain.order.service;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.product.dto.ProductType;

public interface ItemHandler {

    boolean supports(ProductType type);

    /**
     * Handles validation and processing logic for the given item
     */
    ItemHandlerResult handle(OrderItem item);
    
}
