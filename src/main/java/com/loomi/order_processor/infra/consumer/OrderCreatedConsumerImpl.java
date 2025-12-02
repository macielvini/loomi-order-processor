package com.loomi.order_processor.infra.consumer;

import java.util.List;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loomi.order_processor.app.service.OrderProcessPipeline;
import com.loomi.order_processor.domain.order.consumer.OrderCreatedConsumer;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.entity.OrderPendingApprovalEvent;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;
import com.loomi.order_processor.domain.order.exception.OrderNotFoundException;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.usecase.OrderEventIdempotencyService;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedConsumerImpl implements OrderCreatedConsumer {

    private final OrderRepository orderRepository;
    private final OrderProducer producer;
    private final OrderProcessPipeline pipeline;
    private final OrderEventIdempotencyService orderEventIdempotencyService;

    private OrderFailedEvent buildFailedEvent(UUID orderId, List<String> errors) {
        return OrderFailedEvent.fromOrder(orderId, String.join(", ", errors));
    }

    private OrderFailedEvent buildFailedEvent(UUID orderId, String error) {
        return OrderFailedEvent.fromOrder(orderId, error);
    }

    private void failOrder(Order order, List<String> errors) {
        order.status(OrderStatus.FAILED);
        orderRepository.update(order);
        log.error("Order {} failed with reason: {}", order.id(), errors);
        producer.sendOrderFailedEvent(buildFailedEvent(order.id(), errors));
    }
    
    private void requireApprovalOnOrder(Order order) {
        order.status(OrderStatus.PENDING_APPROVAL);
        orderRepository.update(order);
        log.info("Order {} requires manual approval", order.id());
        producer.sendOrderPendingApprovalEvent(OrderPendingApprovalEvent.fromOrder(order.id()));
    }

    private void processOrder(Order order) {
        order.status(OrderStatus.PROCESSED);
        orderRepository.update(order);
        producer.sendOrderProcessedEvent(OrderProcessedEvent.fromOrder(order.id()));
    }

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "orderCreatedListenerFactory")
    @Transactional
    public void handler(OrderCreatedEvent event, Acknowledgment ack) {
        log.info("Received Order Created Event: {}", event);
        UUID orderId = event.getPayload().getId();

        try {
            var idempotencyResult = orderEventIdempotencyService.registerEvent(
                    event.getId(),
                    orderId,
                    event.getType(),
                    event.getPayload().getStatus(),
                    event
            );

            if (idempotencyResult.equals(OrderEventIdempotencyService.Result.ALREADY_PROCESSED)) {
                ack.acknowledge();
                return;
            }

            var order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

            var validations = pipeline.validate(order);
            if (!validations.isValid()) {
                failOrder(order, validations.getErrors());
                ack.acknowledge();
                return;
            }

            if (validations.isHumanReviewRequired()) {
                requireApprovalOnOrder(order);
                ack.acknowledge();
                return;
            }

            var processResult = pipeline.process(order);

            if (processResult.isFailed()) {
                failOrder(order, processResult.getErrors());
                ack.acknowledge();
                return;
            }

            processOrder(order);
            ack.acknowledge();
        } catch (OrderNotFoundException e) {
            log.error("Order not found: {}", orderId);
            throw e;
        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

}
