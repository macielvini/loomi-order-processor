package com.loomi.order_processor.infra.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.producer.OrderProducer;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderProducerImpl implements OrderProducer {

    private final KafkaTemplate<String, Object> template;


    @Value("${kafka.topics.order-events}")
    private String topic;

    @Override
    public void sendOrderCreatedEvent(@NotNull OrderCreatedEvent event) {
        String key = event.getId().toString();
        template.send(topic, key, event);
    }
    
}
