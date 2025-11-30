package com.loomi.order_processor.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemValidationResult {
    private OrderItemValidationError error;

    public boolean isValid() {
        return error == null;
    }

    public static OrderItemValidationResult ok() {
        return OrderItemValidationResult.builder().build();
    }

    public static OrderItemValidationResult error(OrderItemValidationError error) {
        return OrderItemValidationResult.builder().error(error).build();
    }
}
