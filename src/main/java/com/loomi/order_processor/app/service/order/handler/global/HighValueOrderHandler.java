package com.loomi.order_processor.app.service.order.handler.global;

import java.math.BigDecimal;

import com.loomi.order_processor.domain.order.usecase.RequireManualValidationForHighOrderValue;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.app.config.OrderProcessingConfig;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighValueOrderHandler implements OrderHandler, RequireManualValidationForHighOrderValue {

    private final OrderProcessingConfig config;

    @Override
    public ValidationResult validate(Order order) {
        return this.requireManualValidationForHighOrderValue(order);
    }

    @Override
    public OrderProcessResult process(Order order) {
        return OrderProcessResult.ok();
    }

    @Override
    public ValidationResult requireManualValidationForHighOrderValue(Order order)  {
        if (order.totalAmount() == null) {
            log.error("Order {} has null totalAmount", order.id());
            return ValidationResult.fail(OrderError.INTERNAL_ERROR.toString());
        }

        BigDecimal highValueThreshold = config.getHighValueThreshold();
        if (order.totalAmount().compareTo(highValueThreshold) > 0) {
            log.info("Order {} is a high-value order: totalAmount={}", order.id(), order.totalAmount());
            return ValidationResult.requireHumanReview();
        }

        return ValidationResult.ok();
    }
}

