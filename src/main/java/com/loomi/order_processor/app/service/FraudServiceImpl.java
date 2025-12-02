package com.loomi.order_processor.app.service;

import java.math.BigDecimal;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.app.config.OrderProcessingConfig;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.payment.usecase.FraudService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FraudServiceImpl implements FraudService {

    private final OrderProcessingConfig config;

    @Override
    public boolean isFraud(Order order) {
        var fraudProbability = 0.05; // 5%
        BigDecimal fraudThreshold = config.getFraudThreshold();

        if(order.totalAmount().compareTo(fraudThreshold) >= 0) {
            var random = new Random().nextDouble();
            return random <= fraudProbability;
        }
        
        return false;
    }
    
}
