package com.loomi.order_processor.infra.web.dto;

import java.util.List;

import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
   @NotBlank @NotNull String customerId,
   @NotEmpty List<CreateOrderItem> items
) {
    
}
