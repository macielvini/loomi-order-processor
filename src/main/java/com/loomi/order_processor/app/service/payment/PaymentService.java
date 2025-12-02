package com.loomi.order_processor.app.service.payment;

import com.loomi.order_processor.domain.order.entity.Order;

public interface PaymentService {

    void processOrderPayment(Order order);
    
}
