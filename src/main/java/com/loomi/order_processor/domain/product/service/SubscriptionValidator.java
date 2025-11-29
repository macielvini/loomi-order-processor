package com.loomi.order_processor.domain.product.service;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

@Component
@ValidationStrategyFor(ProductType.SUBSCRIPTION)
public class SubscriptionValidator implements ProductValidator {
    
    @Override
    public boolean supports(Product p) {
        return p.productType().equals(ProductType.SUBSCRIPTION);
    }

    @Override
    public ValidationResult validate(Product p) {
        return ValidationResult.ok();
    }

}
