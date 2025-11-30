package com.loomi.order_processor.domain.order.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.ItemHandlerError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.product.dto.ProductType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ItemHandlerFor(value = ProductType.PHYSICAL)
public class WarehouseValidationHandler implements ItemHandler {

    private static final Set<String> WAREHOUSE_LOCATIONS = Set.of("SP", "RJ", "MG");

    @Override
    public ItemHandlerResult handle(OrderItem item) {
        if (item.metadata() == null) {
            log.warn("Missing metadata for product {}", item.productId());
            return ItemHandlerResult.error(ItemHandlerError.WAREHOUSE_UNAVAILABLE);
        }

        var warehouseLocation = item.metadata().get("warehouseLocation");

        if (warehouseLocation == null) {
            log.warn("Missing warehouseLocation in metadata for product {}", item.productId());
            return ItemHandlerResult.error(ItemHandlerError.WAREHOUSE_UNAVAILABLE);
        }

        String location = warehouseLocation.toString().trim().toUpperCase();
        if (location.isEmpty()) {
            log.warn("Empty warehouseLocation in metadata for product {}", item.productId());
            return ItemHandlerResult.error(ItemHandlerError.WAREHOUSE_UNAVAILABLE);
        }
        if (!WAREHOUSE_LOCATIONS.contains(location)) {
            log.warn("Invalid warehouseLocation in metadata for product {}", item.productId());
            return ItemHandlerResult.error(ItemHandlerError.WAREHOUSE_UNAVAILABLE);
        }

        log.debug("Warehouse location validated for product {}: {}", item.productId(), location);
        return ItemHandlerResult.ok();
    }

    @Override
    public boolean supports(ProductType type) {
        return type.equals(ProductType.PHYSICAL);
    }
}

