package com.loomi.order_processor.domain.order.usecase;

public interface CustomerCanBuySubscriptionUseCase {
    boolean customerReachedMaxActiveSubscriptions(String customerId);

    boolean customerAlreadyHaveSubscription(String customerId, String groupId);
}
