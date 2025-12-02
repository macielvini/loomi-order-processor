package com.loomi.order_processor.app.service.order.handler.item;

import com.loomi.order_processor.domain.order.usecase.CalculateDeliveryDays;
import com.loomi.order_processor.app.service.delivery.DeliveryService;
import com.loomi.order_processor.domain.order.usecase.IsItemAvailableForPurchaseUseCase;
import com.loomi.order_processor.domain.order.usecase.NotifyWhenStockIsLowUseCase;
import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.event.usecase.AlertEventPublisher;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalItemHandler implements OrderItemHandler, CalculateDeliveryDays, IsItemAvailableForPurchaseUseCase, NotifyWhenStockIsLowUseCase {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final AlertEventPublisher alertProducer;
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
        if (!this.isItemAvailable(item, product)) {
            log.warn("Product {} is no longer active in order {}", item.productId(), ctx.id());
            return ValidationResult.fail(OrderError.OUT_OF_STOCK.toString());
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        // Stock processing
        int currentStock = product.stockQuantity();
        int remainingStock = currentStock - item.quantity();
        product.stockQuantity(remainingStock);
        productRepository.update(product);

        if (remainingStock < LOW_STOCK_THRESHOLD) {
            log.info("Low stock alert for product {}: remaining stock {} is below threshold {}", 
                    item.productId(), remainingStock, LOW_STOCK_THRESHOLD);
            this.notifyWhenStockIsLow(product);
        }

        // Delivery time calculation
        var location = getWarehouseLocation(item);
        int deliveryDays = this.calculateDeliveryDaysForWarehouse(product, location);
        item.metadata().put("deliveryDays", deliveryDays);

        return OrderProcessResult.ok();
    }

    @Override
    public int calculateDeliveryDaysForWarehouse(Product product, String warehouse) {
        return deliveryService.calculateDeliveryDays(warehouse);
    }

    @Override
    public boolean isItemAvailable(OrderItem item, Product product) {
        if (!product.isActive()) {
            return false;
        }

        if (Objects.isNull(product.stockQuantity()) || product.stockQuantity() < item.quantity()) {;
            return false;
        }

        return true;
    }

    @Override
    public void notifyWhenStockIsLow(Product product) {
        var alertEvent = LowStockAlertEvent.fromProduct(
                product.id(),
                product.stockQuantity(),
                LOW_STOCK_THRESHOLD);
        alertProducer.sendLowStockAlert(alertEvent);
    }
}

