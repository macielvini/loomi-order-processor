package com.loomi.order_processor.domain.order.usecase;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeliveryService {

    private static final Map<String, Integer> DELIVERY_DAYS_BY_LOCATION = Map.of(
        "SP", 5,
        "RJ", 7,
        "MG", 10
    );
    private static final Set<String> WAREHOUSE_LOCATIONS = Set.of("SP", "RJ", "MG");
    private static final int DEFAULT_DELIVERY_DAYS = 10;

    public boolean isValidWarehouseLocation(String location) {
        if (location == null) {
            return false;
        }
        String normalized = location.trim().toUpperCase();
        return WAREHOUSE_LOCATIONS.contains(normalized);
    }

    public int calculateDeliveryDays(String warehouseLocation) {
        if (warehouseLocation == null || warehouseLocation.trim().isEmpty()) {
            log.debug("No warehouse location provided, using default delivery days: {}", DEFAULT_DELIVERY_DAYS);
            return DEFAULT_DELIVERY_DAYS;
        }

        String normalized = warehouseLocation.trim().toUpperCase();
        int deliveryDays = DELIVERY_DAYS_BY_LOCATION.getOrDefault(normalized, DEFAULT_DELIVERY_DAYS);
        
        if (!WAREHOUSE_LOCATIONS.contains(normalized)) {
            log.error("Invalid warehouse location '{}', using default delivery days: {}", warehouseLocation, DEFAULT_DELIVERY_DAYS);
        }
        
        return deliveryDays;
    }
}

