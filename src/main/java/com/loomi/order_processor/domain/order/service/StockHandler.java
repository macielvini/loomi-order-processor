package com.loomi.order_processor.domain.order.service;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.ItemHandlerError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ItemHandlerFor(value = ProductType.PHYSICAL)
public class StockHandler implements ItemHandler {

    private final ProductRepository productRepository;

    @Override
    public ItemHandlerResult handle(OrderItem item) {
        var optProduct = productRepository.findById(item.productId());
        if (optProduct.isEmpty()) {
            return ItemHandlerResult.error(ItemHandlerError.INTERNAL_ERROR);
        }

        var product = optProduct.get();

        if (!product.isActive() || product.stockQuantity() < item.quantity()) {
            return ItemHandlerResult.error(ItemHandlerError.OUT_OF_STOCK);
        }

        return ItemHandlerResult.ok();
    }

    @Override
    public boolean supports(ProductType type) {
        return type.equals(ProductType.PHYSICAL);
    }
    
}
