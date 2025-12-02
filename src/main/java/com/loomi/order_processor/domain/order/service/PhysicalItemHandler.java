package com.loomi.order_processor.domain.order.service;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.producer.AlertProducer;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalItemHandler implements OrderItemHandler {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final AlertProducer alertProducer;
    private final DeliveryService deliveryService;

    private ValidationResult validateMetadata(OrderItem item) {
        if (item.metadata() == null) {
            log.warn("Missing metadata for product {}", item.productId());
            return ValidationResult.fail(OrderError.WAREHOUSE_UNAVAILABLE.toString());
        }

        var warehouseLocation = item.metadata().getOrDefault("warehouseLocation", "").toString();
        if (StringUtils.isBlank(warehouseLocation)) {
            log.warn("Missing warehouseLocation in metadata for product {}", item.productId());
            return ValidationResult.fail(OrderError.WAREHOUSE_UNAVAILABLE.toString());
        }

        return ValidationResult.ok();
    }

    private  String getWarehouseLocation(OrderItem item) {
        var warehouseLocation = item.metadata().get("warehouseLocation");
        return warehouseLocation.toString().trim().toUpperCase();
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PHYSICAL;
    }

    @Override
    public ValidationResult validate(OrderItem item, Product product, Order ctx) {
        // Warehouse validation
        var metadataValidation = validateMetadata(item);
        if (!metadataValidation.isValid()) {
            return metadataValidation;
        }

        String location = getWarehouseLocation(item);

        if (!deliveryService.isValidWarehouseLocation(location)) {
            log.warn("Invalid warehouseLocation in order {} for product {}", ctx.id(), item.productId());
            return ValidationResult.fail(OrderError.WAREHOUSE_UNAVAILABLE.toString());
        }

        // Stock validation
        if (!product.isActive()) {
            log.warn("Product {} is no longer active in order {}", item.productId(), ctx.id());
            return ValidationResult.fail(OrderError.OUT_OF_STOCK.toString());
        }

        if (product.stockQuantity() == null || product.stockQuantity() < item.quantity()) {
            log.warn("Insufficient stock for product {} in order {}: required {}, available {}", 
                    item.productId(), ctx.id(), item.quantity(), product.stockQuantity());
            return ValidationResult.fail(OrderError.OUT_OF_STOCK.toString());
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        // Stock processing
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
        var location = getWarehouseLocation(item);
        // Delivery time calculation
        int deliveryDays = deliveryService.calculateDeliveryDays(location);
        item.metadata().put("deliveryDays", deliveryDays);

        return OrderProcessResult.ok();
    }

}

