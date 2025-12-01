package com.loomi.order_processor.infra.persistence.order;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
    
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);
}

