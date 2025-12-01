package com.loomi.order_processor.domain.order.service;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ItemHandlerFor(value = ProductType.SUBSCRIPTION)
public class SubscriptionProductHandler implements ItemHandler {

    private static final int MAX_ACTIVE_SUBSCRIPTIONS = 5;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    public ItemHandlerResult handle(OrderItem item) {
        var optProduct = productRepository.findById(item.productId());
        if (optProduct.isEmpty()) {
            log.error("Product not found: {}", item.productId());
            return ItemHandlerResult.error(OrderError.INTERNAL_ERROR);
        }

        var product = optProduct.get();

        if (!product.isActive()) {
            log.error("Product is not active: {}", item.productId());
            return ItemHandlerResult.error(OrderError.SUBSCRIPTION_NOT_AVAILABLE);
        }

        if (product.metadata() == null || !product.metadata().containsKey("GROUP_ID")) {
            log.error("Product {} is missing GROUP_ID in metadata", item.productId());
            return ItemHandlerResult.error(OrderError.INTERNAL_ERROR);
        }

        String groupId = product.metadata().get("GROUP_ID").toString();

        var existingSubscriptionsWithSameGroup = orderRepository
                .findActiveSubscriptionsByCustomerIdAndGroupId(item.customerId(), groupId);

        if (!existingSubscriptionsWithSameGroup.isEmpty()) {
            return ItemHandlerResult.error(OrderError.DUPLICATE_ACTIVE_SUBSCRIPTION);
        }

        var allActiveSubscriptions = orderRepository.findAllActiveSubscriptionsByCustomerId(item.customerId());
        long subscriptionCount = allActiveSubscriptions.size();

        if (subscriptionCount >= MAX_ACTIVE_SUBSCRIPTIONS) {
            return ItemHandlerResult.error(OrderError.SUBSCRIPTION_LIMIT_EXCEEDED);
        }

        return ItemHandlerResult.ok();
    }

    @Override
    public boolean supports(ProductType type) {
        return type.equals(ProductType.SUBSCRIPTION);
    }
}

