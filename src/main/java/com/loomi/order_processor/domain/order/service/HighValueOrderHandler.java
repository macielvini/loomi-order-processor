package com.loomi.order_processor.domain.order.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HighValueOrderHandler implements OrderHandler {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");

    @Override
    public ValidationResult validate(Order order) {
        if (order.totalAmount() == null) {
            log.error("Order {} has null totalAmount", order.id());
            return ValidationResult.fail(OrderError.INTERNAL_ERROR.toString());
        }

        if (order.totalAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            // Additional validation logic for orders with value above $10K goes here
            log.info("Order {} is a high-value order: totalAmount={}", order.id(), order.totalAmount());
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(Order order) {
        return OrderProcessResult.ok();
    }
}

