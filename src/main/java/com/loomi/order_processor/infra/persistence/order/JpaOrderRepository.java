package com.loomi.order_processor.infra.persistence.order;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
    
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);

    @Query(value = """
        SELECT DISTINCT o.* FROM orders o
        WHERE o.customer_id = :customerId
        AND o.status = :status
        AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(o.items) AS item
            WHERE item->>'product_type' = 'SUBSCRIPTION'
        )
        """, nativeQuery = true)
    List<Order> findActiveSubscriptionsByCustomerId(
        @Param("customerId") String customerId,
        @Param("status") String status
    );

    @Query(value = """
        SELECT DISTINCT o.* FROM orders o
        INNER JOIN jsonb_array_elements(o.items) AS item ON item->>'product_type' = 'SUBSCRIPTION'
        INNER JOIN products p ON p.id = (item->>'product_id')::uuid
        WHERE o.customer_id = :customerId
        AND o.status = :status
        AND LOWER(p.metadata->>'GROUP_ID') = LOWER(:groupId)
        """, nativeQuery = true)
    List<Order> findActiveSubscriptionsByCustomerIdAndGroupId(
        @Param("customerId") String customerId,
        @Param("status") String status,
        @Param("groupId") String groupId
    );
}

