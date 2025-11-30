package com.loomi.order_processor.infra.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loomi.order_processor.domain.order.consumer.OrderConsumer;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.service.ProcessingResult;
import com.loomi.order_processor.app.service.OrderCreatedProcessorImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumerImpl implements OrderConsumer {

    private final OrderRepository orderRepository;
    private final OrderCreatedProcessorImpl orderProcessor;
    private final OrderProducer orderProducer;

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "orderCreatedListenerFactory")
    @Transactional
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received Order Created Event: {}", event);

        var orderPayload = event.getPayload();
        var orderId = orderPayload.getId();
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.error("Order not found in database: {}", orderId);
            return;
        }
        var order = orderOpt.get();

        try {
            if (order.status() != OrderStatus.PENDING) {
                log.info("Order {} already processed with status: {}. Skipping.", orderId, order.status());
                return;
            }

            ProcessingResult result = orderProcessor.processOrder(order);

            if (result.isSuccess()) {
                order.status(OrderStatus.PROCESSED);
                orderRepository.update(order);

                var processedEvent = OrderProcessedEvent.fromOrder(orderId);
                orderProducer.sendOrderProcessedEvent(processedEvent);
                log.info("Order {} processed successfully", orderId);
            } else {
                order.status(OrderStatus.FAILED);
                orderRepository.update(order);

                var failedEvent = OrderFailedEvent.fromOrder(orderId, result.getFailureReason());
                orderProducer.sendOrderFailedEvent(failedEvent);
                log.warn("Order {} failed with reason: {}", orderId, result.getFailureReason());
            }

        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage(), e);

            try {
                if (order.status() == OrderStatus.PENDING) {
                    order.status(OrderStatus.FAILED);
                    orderRepository.update(order);

                    var failedEvent = OrderFailedEvent.fromOrder(orderId, "PROCESSING_ERROR: " + e.getMessage());
                    orderProducer.sendOrderFailedEvent(failedEvent);
                }
            } catch (Exception updateException) {
                log.error("Failed to update order status after error: {}", updateException.getMessage());
            }
        }
    }

}
