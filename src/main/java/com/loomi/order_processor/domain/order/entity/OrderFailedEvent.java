package com.loomi.order_processor.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loomi.order_processor.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderFailedEvent {
    @JsonProperty("eventId")
    private UUID id;

    @JsonProperty("eventType")
    private OrderEventType type;

    private LocalDateTime timestamp;

    private OrderFailedPayload payload;

    public OrderFailedEvent(OrderFailedPayload payload) {
        this.id = UUID.randomUUID();
        this.type = OrderEventType.ORDER_FAILED;
        this.timestamp = LocalDateTime.now();
        this.payload = payload;
    }

    public static OrderFailedEvent fromOrder(UUID orderId, String reason) {
        var payload = new OrderFailedPayload(orderId, reason, LocalDateTime.now());
        return new OrderFailedEvent(payload);
    }
}

