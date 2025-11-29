package com.loomi.order_processor.domain.product.service;

import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

public interface ProductValidator {

    boolean supports(Product p);

    ValidationResult validate(Product p);

}
