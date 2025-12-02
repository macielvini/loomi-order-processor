package com.loomi.order_processor.infra.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.loomi.order_processor.domain.order.dto.OrderStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderSummaryResponse(
    UUID orderId,
    BigDecimal totalAmount,
    OrderStatus status,
    LocalDateTime createdAt
) {
}

