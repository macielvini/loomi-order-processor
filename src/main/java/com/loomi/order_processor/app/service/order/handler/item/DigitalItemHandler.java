package com.loomi.order_processor.app.service.order.handler.item;

import java.util.*;

import com.loomi.order_processor.domain.order.usecase.CustomerAlreadyOwnsLicenseUseCase;
import com.loomi.order_processor.domain.order.usecase.IsItemAvailableForPurchaseUseCase;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.notification.service.EmailService;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DigitalItemHandler implements OrderItemHandler, IsItemAvailableForPurchaseUseCase, CustomerAlreadyOwnsLicenseUseCase {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    private ArrayList<UUID> licensePool = new ArrayList<>();

    @PostConstruct
    public void populateLicensePool() {
        int poolSize = 10;
        for(int i = 0; i < poolSize; i++) {
            licensePool.add(UUID.randomUUID());
        }
    }

    @Override
    public ProductType supportedType() {
        return ProductType.DIGITAL;
    }

    @Override
    public ValidationResult validate(OrderItem item, Product product, Order ctx) {
        if (!product.isActive() || Objects.isNull(product.stockQuantity())) {
            return ValidationResult.fail(OrderError.DISTRIBUTION_RIGHTS_EXPIRED.toString());
        }

        if (!this.isItemAvailable(item, product)) {
            return ValidationResult.fail(OrderError.LICENSE_UNAVAILABLE.toString());
        }

        if (this.customerAlreadyOwnsLicense(ctx.customerId(), item.productId())) {
            return ValidationResult.fail(OrderError.ALREADY_OWNED.toString());
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        int currentStock = product.stockQuantity();
        int remainingStock = currentStock - item.quantity();

        product.stockQuantity(remainingStock);
        productRepository.update(product);
        log.info("License reserved for product {}: {} units, remaining: {}",
                item.productId(), item.quantity(), remainingStock);

        String licenseKey = licensePool.remove(licensePool.size() - 1).toString();
        String customerEmail = extractEmailFromMetadata(item);
        var emailPayload = createEmailPayload(item, product, licenseKey);
        emailService.sendTo(customerEmail, emailPayload);

        return OrderProcessResult.ok();
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

    @Override
    public boolean isItemAvailable(OrderItem item, Product ctx) {
        return ctx.isActive() && !licensePool.isEmpty() && ctx.stockQuantity() > 0;
    }

    @Override
    public boolean customerAlreadyOwnsLicense(String customerId, UUID productId) {
        return !orderRepository.findByCustomerIdAndProductIdAndStatus(
                customerId,
                productId,
                OrderStatus.PROCESSED).isEmpty();
    }

    private record EmailPayload(String productName, String activationKey, UUID productId) {
    }
}
