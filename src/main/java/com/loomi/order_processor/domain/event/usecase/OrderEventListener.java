package com.loomi.order_processor.domain.event.usecase;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import org.springframework.kafka.support.Acknowledgment;

public interface OrderEventListener {
    void handler(OrderCreatedEvent event, Acknowledgment ack);
}
