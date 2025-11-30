package com.loomi.order_processor.domain.order.producer;

import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;

public interface AlertProducer {

    void sendLowStockAlert(LowStockAlertEvent event);
}

