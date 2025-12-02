package com.loomi.order_processor.domain.payment.usecase;

import com.loomi.order_processor.domain.order.entity.Order;

public interface PaymentService {

    void processOrderPayment(Order order);
    
}
