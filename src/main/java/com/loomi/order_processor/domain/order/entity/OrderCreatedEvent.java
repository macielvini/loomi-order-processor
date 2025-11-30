package com.loomi.order_processor.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.loomi.order_processor.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends OrderEvent<Order> {
    public OrderCreatedEvent(Order order) {
        super(UUID.randomUUID(), OrderEventType.ORDER_CREATED, LocalDateTime.now(), order);
    }
}
