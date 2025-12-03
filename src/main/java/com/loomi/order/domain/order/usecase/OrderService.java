package com.loomi.order.domain.order.usecase;

import java.util.List;
import java.util.UUID;

import com.loomi.order.domain.order.dto.CreateOrder;
import com.loomi.order.domain.order.entity.Order;

public interface OrderService {
     
    Order consultOrder(UUID orderId);

    Order createOrder(CreateOrder createOrder);

    List<Order> findOrdersByCustomerId(String customerId);
}
