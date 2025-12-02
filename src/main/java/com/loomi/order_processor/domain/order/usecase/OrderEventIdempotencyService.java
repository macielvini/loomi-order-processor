package com.loomi.order_processor.domain.order.usecase;

import java.util.UUID;

import com.loomi.order_processor.domain.order.dto.OrderEventType;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;

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



