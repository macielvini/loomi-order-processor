package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.entity.Product;

public interface IsAvailableUseCase {
    boolean isItemAvailable(OrderItem item, Product ctx);
}
