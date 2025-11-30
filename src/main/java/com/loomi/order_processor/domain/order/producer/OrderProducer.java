package com.loomi.order_processor.domain.order.producer;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;

public interface OrderProducer {

    void sendOrderCreatedEvent(OrderCreatedEvent event);
    
}
