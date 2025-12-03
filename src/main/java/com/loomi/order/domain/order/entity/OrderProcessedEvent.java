package com.loomi.order.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loomi.order.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderProcessedEvent {
    @JsonProperty("eventId")
    private UUID id;

    @JsonProperty("eventType")
    private OrderEventType type;

    private LocalDateTime timestamp;

    private OrderProcessedPayload payload;

    public OrderProcessedEvent(OrderProcessedPayload payload) {
        this.id = UUID.randomUUID();
        this.type = OrderEventType.ORDER_PROCESSED;
        this.timestamp = LocalDateTime.now();
        this.payload = payload;
    }

    public static OrderProcessedEvent fromOrder(UUID orderId) {
        var payload = new OrderProcessedPayload(orderId, LocalDateTime.now());
        return new OrderProcessedEvent(payload);
    }
}

