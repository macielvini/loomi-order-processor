package com.loomi.order_processor.infra.web.dto;

import java.util.List;

import com.loomi.order_processor.domain.order.dto.OrderItem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
   @NotBlank String customerId,
   @NotEmpty List<OrderItem> items
) {
    
}
