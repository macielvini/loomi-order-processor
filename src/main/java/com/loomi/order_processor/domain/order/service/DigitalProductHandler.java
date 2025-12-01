package com.loomi.order_processor.domain.order.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.notification.service.EmailService;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ItemHandlerFor(value = ProductType.DIGITAL)
public class DigitalProductHandler implements ItemHandler {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    private static int MAX_LICENSE_PER_ORDER_ITEM = 1;

    @Override
    public ItemHandlerResult handle(OrderItem item) {
        // Set to ONE license per order item
        item.quantity(MAX_LICENSE_PER_ORDER_ITEM); 

        var optProduct = productRepository.findById(item.productId());
        if (optProduct.isEmpty()) {
            log.error("Product not found: {}", item.productId());
            return ItemHandlerResult.error(OrderError.INTERNAL_ERROR);
        }

        var product = optProduct.get();

        // Check distribution rights are still valid
        if (!product.isActive()) {
            return ItemHandlerResult.error(OrderError.DISTRIBUTION_RIGHTS_EXPIRED);
        }

        // Check license availability
        if (product.stockQuantity() == null || product.stockQuantity() < item.quantity()) {
            return ItemHandlerResult.error(OrderError.LICENSE_UNAVAILABLE);
        }

        var existingOrders = orderRepository.findByCustomerIdAndProductIdAndStatus(
                item.customerId(),
                item.productId(),
                OrderStatus.PROCESSED);

        if (!existingOrders.isEmpty()) {
            return ItemHandlerResult.error(OrderError.ALREADY_OWNED);
        }

        int currentStock = product.stockQuantity();
        int remainingStock = currentStock - item.quantity();

        product.stockQuantity(remainingStock);
        productRepository.update(product);
        log.info("License reserved for product {}: {} units, remaining: {}",
                item.productId(), item.quantity(), remainingStock);

        String activationKey = getDigitalLicenseFor(item);
        log.info("Generated activation key for product {}: {}", item.productId(), activationKey);

        String customerEmail = extractEmailFromMetadata(item);
        var emailPayload = createEmailPayload(item, product, activationKey);
        emailService.sendTo(customerEmail, emailPayload);

        return ItemHandlerResult.ok();
    }

    @Override
    public boolean supports(ProductType type) {
        return type.equals(ProductType.DIGITAL);
    }

    private String getDigitalLicenseFor(OrderItem item) {
        return UUID.randomUUID().toString();
    }

    private String extractEmailFromMetadata(OrderItem item) {
        if (item.metadata() != null && item.metadata().containsKey("deliveryEmail")) {
            return item.metadata().get("deliveryEmail").toString();
        }
        return "customer@example.com";
    }

    private Object createEmailPayload(OrderItem item, com.loomi.order_processor.domain.product.entity.Product product,
            String activationKey) {
        return new EmailPayload(product.name(), activationKey, item.productId());
    }

    private record EmailPayload(String productName, String activationKey, UUID productId) {
    }
}
