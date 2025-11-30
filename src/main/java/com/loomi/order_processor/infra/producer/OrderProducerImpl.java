package com.loomi.order_processor.infra.producer;

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

    private static final String TOPIC = "order-events";

    @Override
    public void sendOrderCreatedEvent(@NotNull OrderCreatedEvent event) {
        String key = event.getId().toString();
        template.send(TOPIC, key, event);
    }
    
}
