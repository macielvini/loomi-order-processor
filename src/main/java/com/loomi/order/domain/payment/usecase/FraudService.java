package com.loomi.order.domain.payment.usecase;

import com.loomi.order.domain.order.entity.Order;

public interface FraudService {

    boolean isFraud(Order order);

}
 