package com.loomi.order_processor.domain.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;

public interface OrderRepository {

    Optional<Order> findById(UUID id);

    Order save(Order order);

    List<Order> findAll();

    List<Order> findAll(int limit);

    void update(Order order);

    List<Order> findByCustomerIdAndProductIdAndStatus(String customerId, UUID productId, OrderStatus status);

    List<Order> findActiveSubscriptionsByCustomerIdAndGroupId(String customerId, String groupId);

    List<Order> findAllActiveSubscriptionsByCustomerId(String customerId);
}

