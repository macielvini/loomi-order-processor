package com.loomi.order_processor.domain.order.dto;

public enum OrderEventType {
    ORDER_CREATED,
    ORDER_PROCESSED,
    ORDER_FAILED,
    ORDER_PENDING_APPROVAL,
    LOW_STOCK_ALERT,
}
