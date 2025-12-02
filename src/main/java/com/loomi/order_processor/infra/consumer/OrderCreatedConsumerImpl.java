package com.loomi.order_processor.infra.consumer;

import java.util.List;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loomi.order_processor.app.service.OrderProcessPipeline;
import com.loomi.order_processor.domain.order.consumer.OrderCreatedConsumer;
import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.entity.OrderPendingApprovalEvent;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;
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
    public void handler(OrderCreatedEvent event) {
        log.info("Received Order Created Event: {}", event);
        UUID orderId = event.getPayload().getId();
        try {
            var order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

            var validations = pipeline.validate(order);
            if (!validations.isValid()) {
                failOrder(order, validations.getErrors());
                return;
            }

            if (validations.isHumanReviewRequired()) {
                requireApprovalOnOrder(order);
                return;
            }

            var processResult = pipeline.process(order);

            if (processResult.isFailed()) {
                failOrder(order, processResult.getErrors());
                return;
            }

            processOrder(order);
        } catch (OrderNotFoundException e) {
            log.error("Order not found: {}", orderId);
            return;
        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage(), e);
            producer.sendOrderFailedEvent(buildFailedEvent(orderId, OrderError.INTERNAL_ERROR.toString()));
        }
    }

}
