package com.loomi.order.domain.order.usecase;

import java.util.UUID;

import com.loomi.order.domain.order.dto.OrderEventType;
import com.loomi.order.domain.order.valueobject.OrderStatus;

public interface OrderEventIdempotencyService {

    enum Result {
        OK,
        ALREADY_PROCESSED
    }

    Result registerEvent(
            UUID eventId,
            UUID orderId,
            OrderEventType eventType,
            OrderStatus orderStatusAfter,
            Object payload
    );
}



