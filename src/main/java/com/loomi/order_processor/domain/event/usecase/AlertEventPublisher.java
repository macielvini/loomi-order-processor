package com.loomi.order_processor.domain.event.usecase;

import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;

public interface AlertEventPublisher {

    void sendLowStockAlert(LowStockAlertEvent event);
}

