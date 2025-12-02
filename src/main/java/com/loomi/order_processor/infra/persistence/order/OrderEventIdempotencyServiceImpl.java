package com.loomi.order_processor.infra.persistence.order;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.order_processor.domain.order.dto.OrderEventType;
import com.loomi.order_processor.domain.order.usecase.OrderEventIdempotencyService;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEventIdempotencyServiceImpl implements OrderEventIdempotencyService {

    private final OrderEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Result registerEvent(
            UUID eventId,
            UUID orderId,
            OrderEventType eventType,
            OrderStatus orderStatusAfter,
            Object payload
    ) {
        String jsonPayload = toJson(payload);

        var inserted = repository.insertIfNotExists(
                eventId,
                orderId,
                eventType.name(),
                orderStatusAfter.name(),
                jsonPayload
        );

        if (inserted.isEmpty()) {
            return Result.ALREADY_PROCESSED;
        }

        return Result.OK;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order event payload", e);
        }
    }
}


