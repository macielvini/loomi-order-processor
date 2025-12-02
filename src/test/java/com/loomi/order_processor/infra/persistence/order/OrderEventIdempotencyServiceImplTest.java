package com.loomi.order_processor.infra.persistence.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.order_processor.domain.order.dto.OrderEventType;
import com.loomi.order_processor.domain.order.service.OrderEventIdempotencyService.Result;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventIdempotencyServiceImpl Tests")
class OrderEventIdempotencyServiceImplTest {

    @Mock
    private OrderEventJpaRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventIdempotencyServiceImpl service;

    @Test
    @DisplayName("shouldReturnOk_whenEventIsNew")
    void shouldReturnOk_whenEventIsNew() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderEventType eventType = OrderEventType.ORDER_CREATED;
        OrderStatus status = OrderStatus.PENDING;
        Object payload = new Object();
        String jsonPayload = "{}";

        when(objectMapper.writeValueAsString(payload)).thenReturn(jsonPayload);
        when(repository.insertIfNotExists(
                eq(eventId),
                eq(orderId),
                eq(eventType.name()),
                eq(status.name()),
                eq(jsonPayload)
        )).thenReturn(Optional.of(new OrderEventEntity()));

        Result result = service.registerEvent(eventId, orderId, eventType, status, payload);

        assertEquals(Result.OK, result);
    }

    @Test
    @DisplayName("shouldReturnAlreadyProcessed_whenEventWasAlreadyInserted")
    void shouldReturnAlreadyProcessed_whenEventWasAlreadyInserted() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderEventType eventType = OrderEventType.ORDER_CREATED;
        OrderStatus status = OrderStatus.PENDING;
        Object payload = new Object();
        String jsonPayload = "{}";

        when(objectMapper.writeValueAsString(payload)).thenReturn(jsonPayload);
        when(repository.insertIfNotExists(
                eq(eventId),
                eq(orderId),
                eq(eventType.name()),
                eq(status.name()),
                eq(jsonPayload)
        )).thenReturn(Optional.empty());

        Result result = service.registerEvent(eventId, orderId, eventType, status, payload);

        assertEquals(Result.ALREADY_PROCESSED, result);
    }
}


