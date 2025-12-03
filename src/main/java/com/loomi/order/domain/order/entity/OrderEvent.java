package com.loomi.order.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loomi.order.domain.order.dto.OrderEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent<T> {
    @JsonProperty("eventId")
    private UUID id;

    @JsonProperty("eventType")
    private OrderEventType type;

    private LocalDateTime timestamp;

    private T payload;
}
