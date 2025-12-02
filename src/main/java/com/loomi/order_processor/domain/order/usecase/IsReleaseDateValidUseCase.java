package com.loomi.order_processor.domain.order.usecase;

import com.loomi.order_processor.domain.product.entity.Product;

public interface IsReleaseDateValidUseCase {
    boolean isReleaseDateValidFor(Product product);
}
