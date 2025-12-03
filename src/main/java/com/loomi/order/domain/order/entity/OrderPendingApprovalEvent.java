package com.loomi.order.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loomi.order.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderPendingApprovalEvent {
    @JsonProperty("eventId")
    private UUID id;

    @JsonProperty("eventType")
    private OrderEventType type;

    private LocalDateTime timestamp;

    private UUID orderId;

    public OrderPendingApprovalEvent(UUID orderId) {
        this.id = UUID.randomUUID();
        this.type = OrderEventType.ORDER_PENDING_APPROVAL;
        this.timestamp = LocalDateTime.now();
        this.orderId = orderId;
    }

    public static OrderPendingApprovalEvent fromOrder(UUID orderId) {
        return new OrderPendingApprovalEvent(orderId);
    }
}

