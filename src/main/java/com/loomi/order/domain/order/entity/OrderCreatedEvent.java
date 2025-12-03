package com.loomi.order.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loomi.order.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderCreatedEvent {
    @JsonProperty("eventId")
    private UUID id;

    @JsonProperty("eventType")
    private OrderEventType type;

    private LocalDateTime timestamp;

    private OrderCreatedPayload payload;

    public OrderCreatedEvent(OrderCreatedPayload order) {
        this.id = UUID.randomUUID();
        this.type = OrderEventType.ORDER_CREATED;
        this.timestamp = LocalDateTime.now();
        this.payload = order;
    }

    public static OrderCreatedEvent fromOrder(Order order) {
        var payload = new OrderCreatedPayload(order.id(), order.customerId(), order.status(), order.totalAmount(), order.items());
        return new OrderCreatedEvent(payload); 
    }
}
