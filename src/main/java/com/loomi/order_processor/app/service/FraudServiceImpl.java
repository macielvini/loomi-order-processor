package com.loomi.order_processor.app.service;

import java.math.BigDecimal;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.payment.service.FraudService;

@Service
public class FraudServiceImpl implements FraudService {

    @Override
    public boolean isFraud(Order order) {
        var fraudProbability = 0.05; // 5%
        var minOrderValueToValidate =  BigDecimal.valueOf(20000.00);

        if(order.totalAmount().compareTo(minOrderValueToValidate) > 0) {
            var random = new Random().nextDouble();
            return random <= fraudProbability;
        }
        
        return false;
    }
    
}
