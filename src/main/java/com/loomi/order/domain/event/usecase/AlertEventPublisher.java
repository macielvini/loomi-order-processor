package com.loomi.order.domain.event.usecase;

import com.loomi.order.domain.order.entity.LowStockAlertEvent;

public interface AlertEventPublisher {

    void sendLowStockAlert(LowStockAlertEvent event);
}

