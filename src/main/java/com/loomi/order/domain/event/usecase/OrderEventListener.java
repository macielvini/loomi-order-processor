package com.loomi.order.domain.event.usecase;

import com.loomi.order.domain.order.entity.OrderCreatedEvent;
import org.springframework.kafka.support.Acknowledgment;

public interface OrderEventListener {
    void handler(OrderCreatedEvent event, Acknowledgment ack);
}
