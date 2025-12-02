package com.loomi.order_processor.domain.order.consumer;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import org.springframework.kafka.support.Acknowledgment;

public interface OrderCreatedConsumer {
    void handler(OrderCreatedEvent event, Acknowledgment ack);
}
