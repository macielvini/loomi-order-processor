package com.loomi.order_processor.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertPayload {
    private UUID productId;
    private Integer currentStock;
    private Integer threshold;
    private LocalDateTime timestamp;
}

