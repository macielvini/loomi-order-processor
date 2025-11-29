package com.loomi.order_processor.domain.product.dto;

import java.util.HashMap;
import java.util.List;

import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.service.ProductValidator;

public class ValidatorMap extends HashMap<ProductType, List<ProductValidator>> {

    public List<ProductValidator> getValidatorsFor(Product p) {
        return getOrDefault(p.productType(), List.of());
    }
    
}
