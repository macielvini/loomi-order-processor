package com.loomi.order.infra.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.loomi.order.domain.order.valueobject.OrderStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderSummaryResponse(
    UUID orderId,
    BigDecimal totalAmount,
    OrderStatus status,
    LocalDateTime createdAt
) {
}

