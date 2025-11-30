package com.loomi.order_processor.domain.payment.service;

import com.loomi.order_processor.domain.order.entity.Order;

public interface FraudService {

    boolean validateOrder(Order order);

}
 