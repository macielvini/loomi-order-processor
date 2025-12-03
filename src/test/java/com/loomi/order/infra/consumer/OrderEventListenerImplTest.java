package com.loomi.order.infra.consumer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.loomi.order.infra.event.consumer.OrderEventListenerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.loomi.order.app.service.order.OrderProcessPipeline;
import com.loomi.order.domain.order.dto.OrderProcessResult;
import com.loomi.order.domain.order.entity.Order;
import com.loomi.order.domain.order.entity.OrderCreatedEvent;
import com.loomi.order.domain.order.entity.OrderCreatedPayload;
import com.loomi.order.domain.order.entity.OrderProcessedEvent;
import com.loomi.order.domain.event.usecase.OrderEventPublisher;
import com.loomi.order.domain.order.repository.OrderRepository;
import com.loomi.order.domain.order.usecase.OrderEventIdempotencyService;
import com.loomi.order.domain.order.valueobject.OrderStatus;
import com.loomi.order.domain.product.dto.ValidationResult;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private OrderProcessPipeline orderProcessPipeline;

    @Mock
    private OrderEventIdempotencyService orderEventIdempotencyService;

    @InjectMocks
    private OrderEventListenerImpl consumer;

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
        when(orderEventIdempotencyService.registerEvent(
                event.getId(), orderId, event.getType(), event.getPayload().getStatus(), event))
                .thenReturn(OrderEventIdempotencyService.Result.OK);

        consumer.handler(event, mock(Acknowledgment.class));

        verify(orderEventPublisher, times(1)).sendOrderProcessedEvent(processedEventCaptor.capture());
    }
}
