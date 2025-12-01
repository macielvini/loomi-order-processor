package com.loomi.order_processor.domain.order.service;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;
import com.loomi.order_processor.domain.order.producer.AlertProducer;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ItemHandlerFor(value = ProductType.PHYSICAL)
public class StockHandler implements ItemHandler {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final AlertProducer alertProducer;

    @Override
    public ItemHandlerResult handle(OrderItem item) {
        var optProduct = productRepository.findById(item.productId());
        if (optProduct.isEmpty()) {
            log.error("Product not found: {}", item.productId());
            return ItemHandlerResult.error(OrderError.INTERNAL_ERROR);
        }

        var product = optProduct.get();

        if (!product.isActive()) {
            log.warn("Product {} is not active", item.productId());
            return ItemHandlerResult.error(OrderError.OUT_OF_STOCK);
        }

        if (product.stockQuantity() == null || product.stockQuantity() < item.quantity()) {
            log.warn("Insufficient stock for product {}: required {}, available {}", 
                    item.productId(), item.quantity(), product.stockQuantity());
            return ItemHandlerResult.error(OrderError.OUT_OF_STOCK);
        }

        int currentStock = product.stockQuantity();
        int remainingStock = currentStock - item.quantity();

        if (remainingStock < LOW_STOCK_THRESHOLD) {
            log.info("Low stock alert for product {}: remaining stock {} is below threshold {}", 
                    item.productId(), remainingStock, LOW_STOCK_THRESHOLD);
            var alertEvent = LowStockAlertEvent.fromProduct(
                    item.productId(), 
                    remainingStock, 
                    LOW_STOCK_THRESHOLD);
            alertProducer.sendLowStockAlert(alertEvent);
        }

        product.stockQuantity(remainingStock);
        productRepository.update(product);
        log.info("Stock reserved for product {}: {} units, remaining: {}", 
                item.productId(), item.quantity(), remainingStock);

        return ItemHandlerResult.ok();
    }

    @Override
    public boolean supports(ProductType type) {
        return type.equals(ProductType.PHYSICAL);
    }
    
}
