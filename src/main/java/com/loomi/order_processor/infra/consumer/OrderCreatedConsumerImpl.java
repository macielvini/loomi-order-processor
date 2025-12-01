package com.loomi.order_processor.infra.consumer;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loomi.order_processor.app.service.OrderProcessPipeline;
import com.loomi.order_processor.domain.order.consumer.OrderCreatedConsumer;
import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.exception.OrderNotFoundException;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedConsumerImpl implements OrderCreatedConsumer {

    private final OrderRepository orderRepository;
    private final OrderProducer producer;
    private final OrderProcessPipeline pipeline;

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "orderCreatedListenerFactory")
    @Transactional
    public void handler(OrderCreatedEvent event) {
        log.info("Received Order Created Event: {}", event);
        UUID orderId = event.getPayload().getId();
        try {
            var order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

            var validations = pipeline.validate(order);
            if (!validations.isValid()) {
                order.status(OrderStatus.FAILED);
                orderRepository.update(order);
                log.error("Order {} failed with reason: {}", orderId, validations.getErrors());
                var failedEvent = OrderFailedEvent.fromOrder(orderId, validations.getErrors().toString());
                producer.sendOrderFailedEvent(failedEvent);
                return;
            }

            if (validations.isHumanReviewRequired()) {
                order.status(OrderStatus.PENDING_APPROVAL);
                orderRepository.update(order);
                log.info("Order {} requires manual approval", orderId);
                var failedEvent = OrderFailedEvent.fromOrder(orderId, OrderError.PENDING_MANUAL_APPROVAL.toString());
                producer.sendOrderFailedEvent(failedEvent);
                return;
            }

            var processResult = pipeline.process(order);

            if (processResult.isFailed()) {
                order.status(OrderStatus.FAILED);
                orderRepository.update(order);
                log.error("Order {} failed with reason: {}", orderId, processResult.getErrors());
                var failedEvent = OrderFailedEvent.fromOrder(orderId, processResult.getErrors().toString());
                producer.sendOrderFailedEvent(failedEvent);
                return;
            }

            order.status(OrderStatus.PROCESSED);
            orderRepository.update(order);
            // Send OrderProcessedEvent
        } catch (OrderNotFoundException e) {
            log.error("Order not found: {}", orderId);
            return;
        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage(), e);
            var failedEvent = OrderFailedEvent.fromOrder(orderId, OrderError.INTERNAL_ERROR.toString());
            producer.sendOrderFailedEvent(failedEvent);
        }
    }

}
