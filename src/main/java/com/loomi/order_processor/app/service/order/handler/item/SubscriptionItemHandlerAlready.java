package com.loomi.order_processor.app.service.order.handler.item;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

import com.loomi.order_processor.domain.order.usecase.CustomerCanBuySubscriptionUseCase;
import com.loomi.order_processor.domain.order.usecase.SubscriptionsInOrderAreCompatibleUseCase;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionItemHandlerAlready implements OrderItemHandler, CustomerCanBuySubscriptionUseCase, SubscriptionsInOrderAreCompatibleUseCase {

    private final OrderRepository orderRepository;
    private static final int MAX_ACTIVE_SUBSCRIPTIONS = 5;
    private static final String GROUP_ID = "GROUP_ID";

    private boolean isMetadataValidForProduct(Product p) {
        return Objects.nonNull(p.metadata()) && !StringUtils.isBlank(p.metadata().getOrDefault(GROUP_ID, "").toString());
    }

    @Override
    public ProductType supportedType() {
        return ProductType.SUBSCRIPTION;
    }

    @Override
    public ValidationResult validate(OrderItem item, Product product, Order ctx) {
        if (!product.isActive()) {
            log.error("Product is not active: {}", item.productId());
            return ValidationResult.fail(OrderError.SUBSCRIPTION_NOT_AVAILABLE.toString());
        }

        if (!this.isMetadataValidForProduct(product)) {
            log.error("Product {} is missing GROUP_ID in metadata", item.productId());
            return ValidationResult.fail(OrderError.INTERNAL_ERROR.toString());
        }

        String groupId = product.metadata().get("GROUP_ID").toString();

        if (this.hasDuplicateSubscriptionInOrder(ctx)) {
            return ValidationResult.fail(OrderError.INCOMPATIBLE_SUBSCRIPTIONS.toString());
        }

        if (this.customerAlreadyHaveSubscription(item.customerId(), groupId)) {
            return ValidationResult.fail(OrderError.DUPLICATE_ACTIVE_SUBSCRIPTION.toString());
        }

        if (this.customerReachedMaxActiveSubscriptions(item.customerId())) {
            return ValidationResult.fail(OrderError.SUBSCRIPTION_LIMIT_EXCEEDED.toString());
        }

        log.info("Subscription item {} from order {} payment scheduled to {}", 
        item.productId(), ctx.id(), LocalDateTime.now().plusMonths(1));

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        log.info("ITEM {} from ORDER {} processed successfully", item.productId(), ctx.id());
        return OrderProcessResult.ok();
    }

    @Override
    public boolean customerReachedMaxActiveSubscriptions(String customerId) {
        var allActiveSubscriptions = orderRepository.findAllActiveSubscriptionsByCustomerId(customerId);
        long subscriptionCount = allActiveSubscriptions.size();

        return subscriptionCount >= MAX_ACTIVE_SUBSCRIPTIONS;
    }

    @Override
    public boolean customerAlreadyHaveSubscription(String customerId, String groupId) {
        var result = orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(customerId, groupId);
        return !result.isEmpty();
    }

    @Override
    public boolean hasDuplicateSubscriptionInOrder(Order order) {
        var subscriptionItems = order.items().stream()
                .filter(i -> i.productType() == ProductType.SUBSCRIPTION)
                .toList();

        var groupIdCounts = subscriptionItems.stream()
                .map(i -> {
                    return i.metadata().get(GROUP_ID);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(gid -> gid, Collectors.counting()));

        // Check for duplicates
        return groupIdCounts.values().stream().anyMatch(count -> count > 1);
    }
}
