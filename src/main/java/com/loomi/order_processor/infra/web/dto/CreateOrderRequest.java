package com.loomi.order_processor.infra.web.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateOrderRequest(
   @NotBlank @NotNull String customerId,
   @NotEmpty List<CreateOrderItem> items
) {
    
}
