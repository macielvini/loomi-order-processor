package com.loomi.order_processor.infra.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.loomi.order_processor.app.service.order.OrderProcessPipeline;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderCreatedPayload;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.infra.consumer.OrderCreatedConsumerImpl;
import com.loomi.order_processor.infra.persistence.order.OrderEventJpaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderEventIdempotencyIntegrationTest {

        @Container
        private static KafkaContainer kafka = new KafkaContainer(
                        DockerImageName.parse("apache/kafka:3.7.0"));

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("order_processor")
                        .withUsername("appuser")
                        .withPassword("apppass");

        @BeforeAll
        static void beforeAll() {
                postgres.start();
        }

        @AfterAll
        static void afterAll() {
                postgres.stop();
        }

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @LocalServerPort
        private int PORT;

        @Autowired
        private OrderCreatedConsumerImpl consumer;

        @Autowired
        private OrderRepository orderRepository;

        @MockitoBean
        private OrderProcessPipeline orderProcessPipeline;

        @MockitoBean
        private OrderProducer orderProducer;

        @Autowired
        private OrderEventJpaRepository orderEventJpaRepository;

        @Test
        void shouldProcessOrderCreatedEventOnlyOnceWhenDuplicated() {
                Order order = Order.builder()
                                .customerId("customer-1")
                                .status(OrderStatus.PENDING)
                                .items(List.<OrderItem>of())
                                .build();

                Order savedOrder = orderRepository.save(order);

                OrderCreatedPayload payload = new OrderCreatedPayload(
                                savedOrder.id(),
                                savedOrder.customerId(),
                                savedOrder.status(),
                                savedOrder.totalAmount(),
                                savedOrder.items());

                UUID eventId = UUID.randomUUID();

                OrderCreatedEvent event = new OrderCreatedEvent(payload);
                event.setId(eventId);

                org.mockito.Mockito.when(orderProcessPipeline.validate(any(Order.class)))
                                .thenReturn(ValidationResult.ok());
                org.mockito.Mockito.when(orderProcessPipeline.process(any(Order.class)))
                                .thenReturn(OrderProcessResult.ok());

                consumer.handler(event, mock(Acknowledgment.class));
                consumer.handler(event, mock(Acknowledgment.class));

                // Verify that the order process pipeline was called only once
                verify(orderProcessPipeline, times(1)).validate(any(Order.class));
                verify(orderProcessPipeline, times(1)).process(any(Order.class));
                verify(orderProducer, times(1)).sendOrderProcessedEvent(any());

                Integer count = orderEventJpaRepository.countByEventId(eventId);

                assertThat(count).isEqualTo(1);
        }

        @Test
        void shouldFailOrderOnlyOnceWhenDuplicatedEvent() {
                Order order = Order.builder()
                                .customerId("customer-failed-1")
                                .status(OrderStatus.PENDING)
                                .items(List.<OrderItem>of())
                                .build();

                Order savedOrder = orderRepository.save(order);

                OrderCreatedPayload payload = new OrderCreatedPayload(
                                savedOrder.id(),
                                savedOrder.customerId(),
                                savedOrder.status(),
                                savedOrder.totalAmount(),
                                savedOrder.items());

                UUID eventId = UUID.randomUUID();

                OrderCreatedEvent event = new OrderCreatedEvent(payload);
                event.setId(eventId);

                org.mockito.Mockito.when(orderProcessPipeline.validate(any(Order.class)))
                                .thenReturn(ValidationResult.fail("validation-error"));

                consumer.handler(event, mock(Acknowledgment.class));
                consumer.handler(event, mock(Acknowledgment.class));

                var failedOrder = orderRepository.findById(savedOrder.id()).orElseThrow();
                assertThat(failedOrder.status()).isEqualTo(OrderStatus.FAILED);

                verify(orderProcessPipeline, times(1)).validate(any(Order.class));
                verify(orderProducer, times(1)).sendOrderFailedEvent(any());

                Integer count = orderEventJpaRepository.countByEventId(eventId);
                assertThat(count).isEqualTo(1);
        }
}
