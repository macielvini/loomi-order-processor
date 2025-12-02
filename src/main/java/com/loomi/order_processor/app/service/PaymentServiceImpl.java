package com.loomi.order_processor.app.service;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.payment.usecase.PaymentService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Override
    public void processOrderPayment(Order order) {
        log.info("Processing payment for order: {}", order.id());
        
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("Payment processed successfully for order: {}", order.id());
    }
}
