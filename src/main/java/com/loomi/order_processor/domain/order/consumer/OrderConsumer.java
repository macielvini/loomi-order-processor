package com.loomi.order_processor.domain.order.consumer;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;

public interface OrderConsumer {
    void handleOrderCreatedEvent(OrderCreatedEvent event);
}
