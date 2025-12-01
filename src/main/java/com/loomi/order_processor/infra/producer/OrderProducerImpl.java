package com.loomi.order_processor.infra.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.entity.OrderPendingApprovalEvent;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;
import com.loomi.order_processor.domain.order.producer.OrderProducer;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderProducerImpl implements OrderProducer {

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
        orderCreatedTemplate.send(orderCreatedTopic, key, event);
    }

    @Override
    public void sendOrderProcessedEvent(@NotNull OrderProcessedEvent event) {
        String key = event.getPayload().getOrderId().toString();
        orderProcessedTemplate.send(orderProcessedTopic, key, event);
    }

    @Override
    public void sendOrderFailedEvent(@NotNull OrderFailedEvent event) {
        String key = event.getPayload().getOrderId().toString();
        orderFailedTemplate.send(orderFailedTopic, key, event);
    }

    @Override
    public void sendOrderPendingApprovalEvent(@NotNull OrderPendingApprovalEvent event) {
        String key = event.getOrderId().toString();
        orderPendingApprovalTemplate.send(orderPendingApprovalTopic, key, event);
    }
}
