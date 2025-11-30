package com.loomi.order_processor.domain.order.service;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.product.dto.ProductType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ItemHandlerFor(value = ProductType.PHYSICAL)
public class DeliveryTimeHandler implements ItemHandler {

    private static final int DELIVERY_DAYS_SP = 5;
    private static final int DELIVERY_DAYS_RJ = 7;
    private static final int DELIVERY_DAYS_DEFAULT = 10;

    @Override
    public ItemHandlerResult handle(OrderItem item) {
        if (item.metadata() == null) {
            log.warn("Missing metadata for product {}, using default delivery time", item.productId());
            logDeliveryTime(item.productId(), DELIVERY_DAYS_DEFAULT);
            return ItemHandlerResult.ok();
        }

        var warehouseLocation = item.metadata().get("warehouseLocation");
        if (warehouseLocation == null) {
            log.warn("Missing warehouseLocation for product {}, using default delivery time", item.productId());
            logDeliveryTime(item.productId(), DELIVERY_DAYS_DEFAULT);
            return ItemHandlerResult.ok();
        }

        String location = warehouseLocation.toString().trim().toUpperCase();
        int deliveryDays = calculateDeliveryDays(location);
        
        log.info("Delivery time calculated for product {}: {} days",item.productId(), deliveryDays);
        return ItemHandlerResult.ok();
    }

    private int calculateDeliveryDays(String warehouseLocation) {
        return switch (warehouseLocation) {
            case "SP" -> DELIVERY_DAYS_SP;
            case "RJ" -> DELIVERY_DAYS_RJ;
            default -> DELIVERY_DAYS_DEFAULT;
        };
    }

    private void logDeliveryTime(java.util.UUID productId, int days) {
        log.info("Delivery time calculated for product {}: {} days", productId, days);
    }

    @Override
    public boolean supports(ProductType type) {
        return type.equals(ProductType.PHYSICAL);
    }
}

