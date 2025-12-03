package com.loomi.order_processor.app.service.order.handler.global;

import java.math.BigDecimal;

import com.loomi.order_processor.domain.order.usecase.IsManualValidationRequiredUseCase;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.app.config.OrderProcessingConfig;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighValueOrderHandler implements OrderHandler, IsManualValidationRequiredUseCase {

    private final OrderProcessingConfig config;

    @Override
    public ValidationResult validate(Order order) {
        return this.checkValueRequiresManualValidation(order.totalAmount()) ? ValidationResult.requireHumanReview() : ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(Order order) {
        return OrderProcessResult.ok();
    }


    @Override
    public boolean checkValueRequiresManualValidation(BigDecimal value) {
        return value.compareTo(config.getHighValueThreshold()) > 0;
    }
}

