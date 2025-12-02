package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.product.entity.Product;

public interface IsProductSoldOutUseCase {
    boolean isProductSoldOut(Product product, int intendedQuantity);
}
