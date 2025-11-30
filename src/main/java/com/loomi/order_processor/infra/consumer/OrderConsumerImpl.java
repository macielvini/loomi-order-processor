package com.loomi.order_processor.infra.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumerImpl {

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "orderCreatedListenerFactory")
    void orderCreatedConsumer(OrderCreatedEvent event) {
        log.info("\n >> Received Order Created Event: {}", event);
    }

}
