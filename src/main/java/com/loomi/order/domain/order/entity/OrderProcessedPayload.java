package com.loomi.order.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProcessedPayload {
    private UUID orderId;
    private LocalDateTime processedAt;
}

