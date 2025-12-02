package com.loomi.order_processor.app.service.order.handler.item;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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
public class SubscriptionItemHandler implements OrderItemHandler {

    private final OrderRepository orderRepository;
    private static final int MAX_ACTIVE_SUBSCRIPTIONS = 5;

    private ValidationResult hasSameSubscriptionGroupInOrder(OrderItem item, Order ctx) {
        // Check if any group id appears more than once among the subscription items in the order
        var subscriptionItems = ctx.items().stream()
            .filter(i -> i.productType() == ProductType.SUBSCRIPTION)
            .collect(Collectors.toList());

        var groupIdCounts = subscriptionItems.stream()
            .map(i -> {
                if (i.metadata() != null && i.metadata().containsKey("GROUP_ID")) {
                    return i.metadata().get("GROUP_ID").toString();
                }
                return null;
            })
            .filter(gid -> gid != null)
            .collect(Collectors.groupingBy(gid -> gid, Collectors.counting()));

        boolean hasDuplicate = groupIdCounts.values().stream().anyMatch(count -> count > 1);

        if (hasDuplicate) {
            return ValidationResult.fail(OrderError.INCOMPATIBLE_SUBSCRIPTIONS.toString());
        }

        return ValidationResult.ok();
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

        if (product.metadata() == null || !product.metadata().containsKey("GROUP_ID")) {
            log.error("Product {} is missing GROUP_ID in metadata", item.productId());
            return ValidationResult.fail(OrderError.INTERNAL_ERROR.toString());
        }

        String groupId = product.metadata().get("GROUP_ID").toString();

        if (!hasSameSubscriptionGroupInOrder(item, ctx).isValid()) {
            return ValidationResult.fail(OrderError.INCOMPATIBLE_SUBSCRIPTIONS.toString());
        }

        var existingSubscriptionsWithSameGroup = orderRepository
                .findActiveSubscriptionsByCustomerIdAndGroupId(item.customerId(), groupId);

        if (!existingSubscriptionsWithSameGroup.isEmpty()) {
            return ValidationResult.fail(OrderError.DUPLICATE_ACTIVE_SUBSCRIPTION.toString());
        }

        var allActiveSubscriptions = orderRepository.findAllActiveSubscriptionsByCustomerId(item.customerId());
        long subscriptionCount = allActiveSubscriptions.size();

        if (subscriptionCount >= MAX_ACTIVE_SUBSCRIPTIONS) {
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
    
}
