package com.loomi.order.domain.payment.usecase;

import com.loomi.order.domain.order.entity.Order;

public interface PaymentService {

    void processOrderPayment(Order order);
    
}
