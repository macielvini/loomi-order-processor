package com.loomi.order_processor.infra.consumer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.app.service.OrderProcessPipeline;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderCreatedPayload;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderProducer orderProducer;

    @Mock
    private OrderProcessPipeline orderProcessPipeline;

    @InjectMocks
    private OrderCreatedConsumerImpl consumer;

    @Captor
    private ArgumentCaptor<OrderProcessedEvent> processedEventCaptor;

    @Test
    void handler_shouldCallSendOrderProcessedEvent_whenProcessIsSuccessful() {
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .customerId("customer-1")
                .status(OrderStatus.PENDING)
                .items(List.of())
                .build();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.id(),
                order.customerId(),
                order.status(),
                order.totalAmount(),
                order.items());

        OrderCreatedEvent event = new OrderCreatedEvent(payload);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderProcessPipeline.validate(order)).thenReturn(ValidationResult.ok());
        when(orderProcessPipeline.process(order)).thenReturn(OrderProcessResult.ok());

        consumer.handler(event);

        verify(orderProducer, times(1)).sendOrderProcessedEvent(processedEventCaptor.capture());
    }
}


