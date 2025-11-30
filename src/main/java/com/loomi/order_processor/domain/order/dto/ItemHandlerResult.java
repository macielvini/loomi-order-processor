package com.loomi.order_processor.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemHandlerResult {
    private OrderItemValidationError error;

    public boolean isValid() {
        return error == null;
    }

    public static ItemHandlerResult ok() {
        return ItemHandlerResult.builder().build();
    }

    public static ItemHandlerResult error(OrderItemValidationError error) {
        return ItemHandlerResult.builder().error(error).build();
    }
}
