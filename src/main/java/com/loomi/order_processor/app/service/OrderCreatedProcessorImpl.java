package com.loomi.order_processor.app.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.service.OrderCreatedProcessor;
import com.loomi.order_processor.domain.order.service.ProcessingResult;
import com.loomi.order_processor.domain.payment.service.FraudService;
import com.loomi.order_processor.domain.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreatedProcessorImpl implements OrderCreatedProcessor {

    private final PaymentService paymentService;
    private final FraudService fraudService;

    @Value("${order-processing.high-value-threshold:10000.00}")
    private BigDecimal highValueThreshold;

    @Value("${order-processing.fraud-threshold:20000.00}")
    private BigDecimal fraudThreshold;

    private ProcessingResult checkThresholds(Order order) {
        if (order.totalAmount().compareTo(highValueThreshold) > 0) {
            log.info("High-value order detected for order: {} with amount: {} (threshold: {})",
                    order.id(),
                    order.totalAmount(),
                    highValueThreshold);
        }

        if (order.totalAmount().compareTo(fraudThreshold) > 0) {
            if (!fraudService.validateOrder(order)) {
                log.warn("Fraud Alert - Order ID: {}", order.id());
                return ProcessingResult.failure("FRAUD_DETECTED");
            }
        }

        return ProcessingResult.success();
    }

    @Override
    public ProcessingResult processOrder(Order order) {
        log.info("Started processing with Global Order Processor for order: {}", order.id());

        var thresholdsResult = checkThresholds(order);

        if (!thresholdsResult.isSuccess()) {
            return thresholdsResult;
        }

        try {
            paymentService.processOrderPayment(order);
        } catch (Exception e) {
            log.error("Payment processing failed for order: {}", order.id(), e);
            return ProcessingResult.failure("PAYMENT_FAILED");
        }

        log.info("Global processing completed successfully for order: {}", order.id());
        return ProcessingResult.success();
    }

    public ProcessingResult processOrderWithResult(Order order) {
        log.info("Started processing order: {}", order.id());

        if (order.totalAmount().compareTo(highValueThreshold) > 0) {
            log.info("High-value order detected: {} (threshold: {})", order.totalAmount(), highValueThreshold);
        }

        try {
            paymentService.processOrderPayment(order);
        } catch (Exception e) {
            log.error("Payment processing failed for order: {}", order.id(), e);
            return ProcessingResult.failure("PAYMENT_FAILED");
        }

        if (order.totalAmount().compareTo(fraudThreshold) > 0) {
            if (!fraudService.validateOrder(order)) {
                log.warn("Fraud Alert - Order ID: {}", order.id());
                return ProcessingResult.failure("FRAUD_DETECTED");
            }
        } else {
            if (!fraudService.validateOrder(order)) {
                log.warn("Fraud validation failed for order: {}", order.id());
                return ProcessingResult.failure("FRAUD_DETECTED");
            }
        }

        log.info("Global processing completed successfully for order: {}", order.id());
        return ProcessingResult.success();
    }
}
