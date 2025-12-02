package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

public interface RequireManualValidationForHighOrderValue {
    ValidationResult requireManualValidationForHighOrderValue(Order order);
}
