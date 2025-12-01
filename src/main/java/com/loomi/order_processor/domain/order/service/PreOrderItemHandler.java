package com.loomi.order_processor.domain.order.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreOrderItemHandler implements OrderItemHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int CANCELLATION_DAYS_BEFORE_RELEASE = 7;

    private final DeliveryService deliveryService;

    private Optional<LocalDate> extractReleaseDate(Product product) {
        if (product.metadata() == null || product.metadata().get("releaseDate") == null) {
            throw new IllegalArgumentException("Missing releaseDate in product metadata");
        }

        String releaseDateStr = product.metadata().get("releaseDate").toString();
        if (StringUtils.isBlank(releaseDateStr)) {
            throw new IllegalArgumentException("Empty releaseDate in product metadata");
        }

        try {
            return Optional.of(LocalDate.parse(releaseDateStr, DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PRE_ORDER;
    }

    @Override
    public ValidationResult validate(OrderItem item, Product product, Order ctx) {
        if (!product.isActive()) {
            log.error("Product {} is not active for order {}", item.productId(), ctx.id());
            return ValidationResult.fail(OrderError.INVALID_RELEASE_DATE.toString());
        }

        var optReleaseDate = extractReleaseDate(product);
        if (optReleaseDate.isEmpty()) {
            log.error("Failed to extract releaseDate for product {} in order {}",
                    item.productId(), ctx.id());
            return ValidationResult.fail(OrderError.INVALID_RELEASE_DATE.toString());
        }
        LocalDate releaseDate = optReleaseDate.get();

        LocalDate today = LocalDate.now();
        if (!releaseDate.isAfter(today)) {
            return ValidationResult.fail(OrderError.RELEASE_DATE_PASSED.toString());
        }

        if (product.stockQuantity() == null || product.stockQuantity() < item.quantity()) {
            return ValidationResult.fail(OrderError.PRE_ORDER_SOLD_OUT.toString());
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        var optReleaseDate = extractReleaseDate(product);
        if (optReleaseDate.isEmpty()) {
            log.error("Failed to extract releaseDate for product {} in order {}",
                    item.productId(), ctx.id());
            return OrderProcessResult.fail(OrderError.INVALID_RELEASE_DATE.toString());
        }
        LocalDate releaseDate = optReleaseDate.get();
        String releaseDateStr = releaseDate.format(DATE_FORMATTER);

        if (item.metadata() == null) {
            item.metadata(new RawProductMetadata());
        }

        LocalDate maxCancellationDate = releaseDate.minusDays(CANCELLATION_DAYS_BEFORE_RELEASE);
        item.metadata().put("maxCancellationDate", maxCancellationDate.format(DATE_FORMATTER));
        item.metadata().put("releaseDate", releaseDateStr);

        if (item.metadata().containsKey("warehouseLocation")) {
            String warehouseLocation = item.metadata().get("warehouseLocation").toString();
            int deliveryDays = deliveryService.calculateDeliveryDays(warehouseLocation);
            item.metadata().put("deliveryDays", deliveryDays);
            log.info("Calculated delivery days for pre-order item {}: {} days from warehouse {}",
                    item.productId(), deliveryDays, warehouseLocation);
        }

        if (product.metadata() != null && product.metadata().containsKey("preOrderDiscount")) {
            BigDecimal discount = new BigDecimal(
                product.metadata().getOrDefault("preOrderDiscount", "0").toString().trim());
            item.price(item.price().subtract(discount));
            log.info(
                    "Applied pre-order discount for product {}: ${} -> ${}",
                    item.productId(), item.price(), discount);
        }

        return OrderProcessResult.ok();
    }
}
