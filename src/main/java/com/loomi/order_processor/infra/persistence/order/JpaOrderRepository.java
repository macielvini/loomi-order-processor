package com.loomi.order_processor.infra.persistence.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loomi.order_processor.domain.order.entity.Order;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
    
}

