package com.loomi.order_processor.domain.order.service;

import java.util.List;
import java.util.UUID;

import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.entity.Order;

public interface OrderService {
     
    Order consultOrder(UUID orderId);

    Order createOrder(CreateOrder createOrder);

    List<Order> findOrdersByCustomerId(String customerId);
}
