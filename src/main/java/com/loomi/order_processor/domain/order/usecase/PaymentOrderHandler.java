package com.loomi.order_processor.domain.order.usecase;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.payment.usecase.FraudService;
import com.loomi.order_processor.domain.payment.usecase.PaymentService;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderHandler implements OrderHandler {

    private final FraudService fraudService;
    private final PaymentService paymentService;

    @Override
    public ValidationResult validate(Order order) {
        if (order.totalAmount() == null) {
            log.error("Order {} has null totalAmount", order.id());
            return ValidationResult.fail(OrderError.INTERNAL_ERROR.toString());
        }

        boolean isFraud = fraudService.isFraud(order);
        
        if (isFraud) {
            log.warn("Order {} flagged for fraud review: totalAmount={}", order.id(), order.totalAmount());
            return ValidationResult.requireHumanReview();
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(Order order) {
        try {
            paymentService.processOrderPayment(order);
            log.info("Payment processed successfully for order {}", order.id());
            return OrderProcessResult.ok();
        } catch (Exception e) {
            log.error("Payment processing failed for order {}: {}", order.id(), e.getMessage(), e);
            return OrderProcessResult.fail(OrderError.INTERNAL_ERROR.toString());
        }
    }
}

