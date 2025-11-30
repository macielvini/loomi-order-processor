package com.loomi.order_processor.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loomi.order_processor.domain.order.dto.OrderEventType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LowStockAlertEvent {
    @JsonProperty("eventId")
    private UUID id;

    @JsonProperty("eventType")
    private OrderEventType type;

    private LocalDateTime timestamp;

    private LowStockAlertPayload payload;

    public LowStockAlertEvent(LowStockAlertPayload payload) {
        this.id = UUID.randomUUID();
        this.type = OrderEventType.LOW_STOCK_ALERT;
        this.timestamp = LocalDateTime.now();
        this.payload = payload;
    }

    public static LowStockAlertEvent fromProduct(UUID productId, Integer currentStock, Integer threshold) {
        var payload = new LowStockAlertPayload(productId, currentStock, threshold, LocalDateTime.now());
        return new LowStockAlertEvent(payload);
    }
}

