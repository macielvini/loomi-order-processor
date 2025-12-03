package com.loomi.order.app.service.order.handler;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.loomi.order.domain.notification.usecase.EmailService;
import com.loomi.order.domain.order.dto.OrderProcessResult;
import com.loomi.order.domain.order.entity.Order;
import com.loomi.order.domain.order.repository.OrderRepository;
import com.loomi.order.domain.order.valueobject.OrderError;
import com.loomi.order.domain.order.valueobject.OrderItem;
import com.loomi.order.domain.order.valueobject.OrderStatus;
import com.loomi.order.domain.product.dto.ProductType;
import com.loomi.order.domain.product.dto.ValidationResult;
import com.loomi.order.domain.product.entity.Product;
import com.loomi.order.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DigitalItemHandler implements OrderItemHandler {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    private static int MAX_LICENSE_PER_ORDER_ITEM = 1;

    @Override
    public ProductType supportedType() {
        return ProductType.DIGITAL;
    }

    @Override
    public ValidationResult validate(OrderItem item, Product product, Order ctx) {
        if (!product.isActive()) {
            log.error("Product is not active: {}", item.productId());
            return ValidationResult.fail(OrderError.DISTRIBUTION_RIGHTS_EXPIRED.toString());
        }

        if (product.stockQuantity() == null || product.stockQuantity() < MAX_LICENSE_PER_ORDER_ITEM) {
            log.error("License unavailable for product: {}", item.productId());
            return ValidationResult.fail(OrderError.LICENSE_UNAVAILABLE.toString());
        }

        var customerAlreadyOwns = !orderRepository.findByCustomerIdAndProductIdAndStatus(
                item.customerId(),
                item.productId(),
                OrderStatus.PROCESSED).isEmpty();

        if (customerAlreadyOwns) {
            log.error("Customer already owns product: {}", item.productId());
            return ValidationResult.fail(OrderError.ALREADY_OWNED.toString());
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        item.quantity(MAX_LICENSE_PER_ORDER_ITEM);

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

        return OrderProcessResult.ok();
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

    private Object createEmailPayload(OrderItem item, com.loomi.order.domain.product.entity.Product product,
            String activationKey) {
        return new EmailPayload(product.name(), activationKey, item.productId());
    }

    private record EmailPayload(String productName, String activationKey, UUID productId) {
    }
}
