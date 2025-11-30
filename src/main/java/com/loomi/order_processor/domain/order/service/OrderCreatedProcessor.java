package com.loomi.order_processor.domain.order.service;

import com.loomi.order_processor.domain.order.entity.Order;

public interface OrderCreatedProcessor {
    ProcessingResult processOrder(Order order);
}
