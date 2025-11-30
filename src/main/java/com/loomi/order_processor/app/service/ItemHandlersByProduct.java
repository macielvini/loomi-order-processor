package com.loomi.order_processor.app.service;

import java.util.List;
import java.util.Map;

import com.loomi.order_processor.domain.order.service.ItemHandler;
import com.loomi.order_processor.domain.product.dto.ProductType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemHandlersByProduct {
    private final Map<ProductType, List<ItemHandler>> handlers;

    public List<ItemHandler> getHandlersFor(ProductType type) {
        return handlers.getOrDefault(type, List.of());
    }

}
