package com.loomi.order.infra.persistence.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventJpaRepository extends JpaRepository<OrderEventEntity, Long> {

    @Query(value = """
        WITH inserted AS (
            INSERT INTO order_events (event_id, order_id, event_type, order_status, payload)
            VALUES (:eventId, :orderId, :eventType, :orderStatus, CAST(:payload AS jsonb))
            ON CONFLICT (event_id) DO NOTHING
            RETURNING *
        )
        SELECT * FROM inserted
        """, nativeQuery = true)
    Optional<OrderEventEntity> insertIfNotExists(
            @Param("eventId") UUID eventId,
            @Param("orderId") UUID orderId,
            @Param("eventType") String eventType,
            @Param("orderStatus") String orderStatus,
            @Param("payload") String payload
    );

    Integer countByEventId(UUID eventId);
}

