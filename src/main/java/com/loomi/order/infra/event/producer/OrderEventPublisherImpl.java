package com.loomi.order.infra.event.producer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.loomi.order.domain.order.entity.OrderCreatedEvent;
import com.loomi.order.domain.order.entity.OrderFailedEvent;
import com.loomi.order.domain.order.entity.OrderPendingApprovalEvent;
import com.loomi.order.domain.order.entity.OrderProcessedEvent;
import com.loomi.order.domain.event.usecase.OrderEventPublisher;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEventPublisherImpl implements OrderEventPublisher {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final KafkaTemplate<String, OrderCreatedEvent> orderCreatedTemplate;
    private final KafkaTemplate<String, OrderProcessedEvent> orderProcessedTemplate;
    private final KafkaTemplate<String, OrderFailedEvent> orderFailedTemplate;
    private final KafkaTemplate<String, OrderPendingApprovalEvent> orderPendingApprovalTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-processed}")
    private String orderProcessedTopic;

    @Value("${kafka.topics.order-failed}")
    private String orderFailedTopic;

    @Value("${kafka.topics.order-pending-approval}")
    private String orderPendingApprovalTopic;

    @Override
    public void sendOrderCreatedEvent(@NotNull OrderCreatedEvent event) {
        String key = event.getId().toString();
        ProducerRecord<String, OrderCreatedEvent> record = createProducerRecord(orderCreatedTopic, key, event);
        orderCreatedTemplate.send(record);
    }

    @Override
    public void sendOrderProcessedEvent(@NotNull OrderProcessedEvent event) {
        String key = event.getPayload().getOrderId().toString();
        ProducerRecord<String, OrderProcessedEvent> record = createProducerRecord(orderProcessedTopic, key, event);
        orderProcessedTemplate.send(record);
    }

    @Override
    public void sendOrderFailedEvent(@NotNull OrderFailedEvent event) {
        String key = event.getPayload().getOrderId().toString();
        ProducerRecord<String, OrderFailedEvent> record = createProducerRecord(orderFailedTopic, key, event);
        orderFailedTemplate.send(record);
    }

    @Override
    public void sendOrderPendingApprovalEvent(@NotNull OrderPendingApprovalEvent event) {
        String key = event.getOrderId().toString();
        ProducerRecord<String, OrderPendingApprovalEvent> record = createProducerRecord(orderPendingApprovalTopic, key, event);
        orderPendingApprovalTemplate.send(record);
    }

    private <T> ProducerRecord<String, T> createProducerRecord(String topic, String key, T value) {
        RecordHeaders headers = new RecordHeaders();
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return new ProducerRecord<>(topic, null, key, value, headers);
    }
}
