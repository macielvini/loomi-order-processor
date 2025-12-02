package com.loomi.order_processor.infra.web.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrdersListResponse(
    List<OrderSummaryResponse> orders
) {
}

