package com.loomi.order_processor.infra.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;
import com.loomi.order_processor.domain.event.usecase.AlertEventPublisher;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertProducerImpl implements AlertEventPublisher {

    private final KafkaTemplate<String, LowStockAlertEvent> lowStockAlertTemplate;

    @Value("${kafka.topics.low-stock-alert}")
    private String lowStockAlertTopic;

    @Override
    public void sendLowStockAlert(@NotNull LowStockAlertEvent event) {
        String key = event.getPayload().getProductId().toString();
        lowStockAlertTemplate.send(lowStockAlertTopic, key, event);
    }
}

