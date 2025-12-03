package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;

public interface CorporateOrderValidationUseCase {

    boolean orderHasRequiredCustomerInformation(OrderItem item);

    boolean shouldApplyDiscount(OrderItem item);

    boolean hasEnoughCreditForOrder(Order order);
}
