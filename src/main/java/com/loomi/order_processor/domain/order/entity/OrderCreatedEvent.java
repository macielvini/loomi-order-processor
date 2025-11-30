package com.loomi.order_processor.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.loomi.order_processor.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends OrderEvent<OrderCreatedPayload> {
    public OrderCreatedEvent(OrderCreatedPayload order) {
        super(UUID.randomUUID(), OrderEventType.ORDER_CREATED, LocalDateTime.now(), order);
    }

    public static OrderCreatedEvent fromOrder(Order order) {
        var payload = new OrderCreatedPayload(order.id(), order.customerId(), order.status(), order.totalAmount(), order.items());
        return new OrderCreatedEvent(payload);
    }
}
