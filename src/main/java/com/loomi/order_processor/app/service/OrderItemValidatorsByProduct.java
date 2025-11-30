package com.loomi.order_processor.app.service;

import java.util.List;
import java.util.Map;

import com.loomi.order_processor.domain.order.service.OrderItemValidator;
import com.loomi.order_processor.domain.product.dto.ProductType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderItemValidatorsByProduct {
    private final Map<ProductType, List<OrderItemValidator>> validators;

    public List<OrderItemValidator> getValidatorsFor(ProductType type) {
        return validators.getOrDefault(type, List.of());
    }

}
