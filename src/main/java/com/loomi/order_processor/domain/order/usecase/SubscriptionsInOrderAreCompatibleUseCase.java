package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.order.entity.Order;

public interface SubscriptionsInOrderAreCompatibleUseCase {
    boolean hasDuplicateSubscriptionInOrder(Order order);
}

