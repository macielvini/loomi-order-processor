package com.loomi.order.infra.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.loomi.order.domain.order.dto.CreateOrderItem;
import com.loomi.order.domain.order.entity.Order;
import com.loomi.order.domain.event.usecase.AlertEventPublisher;
import com.loomi.order.domain.event.usecase.OrderEventPublisher;
import com.loomi.order.domain.order.repository.OrderRepository;
import com.loomi.order.domain.order.valueobject.OrderStatus;
import com.loomi.order.domain.payment.usecase.FraudService;
import com.loomi.order.domain.product.dto.RawProductMetadata;
import com.loomi.order.domain.product.entity.Product;
import com.loomi.order.domain.product.repository.ProductRepository;
import com.loomi.order.infra.web.dto.CreateOrderRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderEndToEndIntegrationTest {

        @Container
        private static KafkaContainer kafka = new KafkaContainer(
                        DockerImageName.parse("apache/kafka:3.7.0"));

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("order")
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
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoSpyBean
    private OrderEventPublisher orderEventPublisher;

    @MockitoSpyBean
    private FraudService fraudService;

    @MockitoSpyBean
    private AlertEventPublisher alertProducer;

    private ProductRepositoryTestUtils productRepositoryUtils;

    @BeforeEach
    void setUp() {
        productRepositoryUtils = new ProductRepositoryTestUtils(productRepository);
        reset(orderEventPublisher, fraudService, alertProducer);
    }

    @Test
    @DisplayName("Should process a physical order end-to-end from API to PROCESSED status")
    void shouldProcessPhysicalOrderEndToEnd() {
        Product product = productRepositoryUtils.createPhysicalProduct();

        RawProductMetadata itemMetadata = new RawProductMetadata();
        itemMetadata.put("warehouseLocation", "SP");

        CreateOrderItem item = CreateOrderItem.builder()
                .productId(product.id())
                .quantity(1)
                .metadata(itemMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-123",
                List.of(item)
        );

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order processed = orderRepository.findById(orderId).orElseThrow();
                    assertThat(processed.status()).isEqualTo(OrderStatus.PROCESSED);
                    assertThat(processed.totalAmount()).isEqualByComparingTo(product.price());
                });

        verify(orderEventPublisher, atLeastOnce()).sendOrderProcessedEvent(any());
    }

    @Test
    @DisplayName("Should send low stock alert when remaining stock for a physical product is below threshold")
    void shouldSendLowStockAlertWhenRemainingStockIsBelowThresholdEndToEnd() {
        Product product = productRepositoryUtils.createPhysicalProduct();
        product.stockQuantity(6);
        productRepository.update(product);

        RawProductMetadata itemMetadata = new RawProductMetadata();
        itemMetadata.put("warehouseLocation", "SP");

        CreateOrderItem item = CreateOrderItem.builder()
                .productId(product.id())
                .quantity(2)
                .metadata(itemMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-low-stock-alert",
                List.of(item)
        );

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order processed = orderRepository.findById(orderId).orElseThrow();
                    assertThat(processed.status()).isEqualTo(OrderStatus.PROCESSED);
                });

        verify(alertProducer, atLeastOnce()).sendLowStockAlert(any());
    }

    @Test
    @DisplayName("Should mark order as FAILED and publish OrderFailedEvent when at least one item fails")
    void shouldMarkOrderAsFailedWhenAtLeastOneItemFailsEndToEnd() {
        Product okProduct = productRepositoryUtils.createPhysicalProduct();
        Product failingProduct = productRepositoryUtils.createPhysicalProduct();
        failingProduct.stockQuantity(1);
        productRepository.update(failingProduct);

        RawProductMetadata itemMetadata = new RawProductMetadata();
        itemMetadata.put("warehouseLocation", "SP");

        CreateOrderItem okItem = CreateOrderItem.builder()
                .productId(okProduct.id())
                .quantity(1)
                .metadata(itemMetadata)
                .build();

        CreateOrderItem failingItem = CreateOrderItem.builder()
                .productId(failingProduct.id())
                .quantity(5)
                .metadata(itemMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-out-of-stock",
                List.of(okItem, failingItem)
        );

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order failed = orderRepository.findById(orderId).orElseThrow();
                    assertThat(failed.status()).isEqualTo(OrderStatus.FAILED);
                });

        verify(orderEventPublisher, atLeastOnce()).sendOrderFailedEvent(any());
    }

    @Test
    @DisplayName("Should process high value physical order without fraud end-to-end")
    void shouldProcessHighValuePhysicalOrderWithoutFraudEndToEnd() {
        Product product = productRepositoryUtils.createHighValuePhysicalProduct();

        RawProductMetadata itemMetadata = new RawProductMetadata();
        itemMetadata.put("warehouseLocation", "SP");

        CreateOrderItem item = CreateOrderItem.builder()
                .productId(product.id())
                .quantity(1)
                .metadata(itemMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-high-value",
                List.of(item)
        );

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order processed = orderRepository.findById(orderId).orElseThrow();
                    assertThat(processed.status()).isEqualTo(OrderStatus.PROCESSED);
                    assertThat(processed.totalAmount()).isEqualByComparingTo(product.price());
                });

        verify(orderEventPublisher, atLeastOnce()).sendOrderProcessedEvent(any());
        verify(orderEventPublisher, never()).sendOrderFailedEvent(any());
        verify(orderEventPublisher, never()).sendOrderPendingApprovalEvent(any());
    }

    @Test
    @DisplayName("Should mark high value order as PENDING_APPROVAL and publish pending approval event when fraud is detected")
    void shouldMarkHighValueOrderAsPendingApprovalWhenFraudDetectedEndToEnd() {
        Product product = productRepositoryUtils.createHighValuePhysicalProduct();

        RawProductMetadata itemMetadata = new RawProductMetadata();
        itemMetadata.put("warehouseLocation", "SP");

        CreateOrderItem item = CreateOrderItem.builder()
                .productId(product.id())
                .quantity(1)
                .metadata(itemMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-high-value-fraud",
                List.of(item)
        );

        doReturn(true).when(fraudService).isFraud(any(Order.class));

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order pending = orderRepository.findById(orderId).orElseThrow();
                    assertThat(pending.status()).isEqualTo(OrderStatus.PENDING_APPROVAL);
                });

        verify(orderEventPublisher, atLeastOnce()).sendOrderPendingApprovalEvent(any());
        verify(orderEventPublisher, never()).sendOrderProcessedEvent(any());
        verify(orderEventPublisher, never()).sendOrderFailedEvent(any());
    }

    @Test
    @DisplayName("Should process a mixed order with multiple product types end-to-end")
    void shouldProcessMixedOrderWithMultipleProductTypesEndToEnd() {
        Product physical = productRepositoryUtils.createPhysicalProduct();
        Product subscription = productRepositoryUtils.createSubscriptionProductForMixedOrder();
        Product digital = productRepositoryUtils.createDigitalProductForMixedOrder();
        Product preOrder = productRepositoryUtils.createPreOrderProductForMixedOrder();

        RawProductMetadata physicalMetadata = new RawProductMetadata();
        physicalMetadata.put("warehouseLocation", "SP");

        RawProductMetadata subscriptionMetadata = new RawProductMetadata();
        subscriptionMetadata.put("billingCycle", "MONTHLY");

        RawProductMetadata digitalMetadata = new RawProductMetadata();
        digitalMetadata.put("deliveryEmail", "customer-mixed@example.com");
        digitalMetadata.put("format", "PDF");

        RawProductMetadata preOrderMetadata = new RawProductMetadata();

        CreateOrderItem physicalItem = CreateOrderItem.builder()
                .productId(physical.id())
                .quantity(1)
                .metadata(physicalMetadata)
                .build();

        CreateOrderItem subscriptionItem = CreateOrderItem.builder()
                .productId(subscription.id())
                .quantity(1)
                .metadata(subscriptionMetadata)
                .build();

        CreateOrderItem digitalItem = CreateOrderItem.builder()
                .productId(digital.id())
                .quantity(1)
                .metadata(digitalMetadata)
                .build();

        CreateOrderItem preOrderItem = CreateOrderItem.builder()
                .productId(preOrder.id())
                .quantity(1)
                .metadata(preOrderMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-mixed-001",
                List.of(physicalItem, subscriptionItem, digitalItem, preOrderItem)
        );

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order processed = orderRepository.findById(orderId).orElseThrow();
                    assertThat(processed.status()).isEqualTo(OrderStatus.PROCESSED);
                });

        verify(orderEventPublisher, atLeastOnce()).sendOrderProcessedEvent(any());
    }

    @Test
    @DisplayName("Should mark corporate high value order as PENDING_APPROVAL and publish pending approval event")
    void shouldMarkCorporateHighValueOrderAsPendingApprovalEndToEnd() {
        Product corporateProduct = productRepositoryUtils.createCorporateProductForPendingApproval();

        RawProductMetadata corporateMetadata = new RawProductMetadata();
        corporateMetadata.put("cnpj", "12.345.678/0001-90");
        corporateMetadata.put("paymentTerms", "NET_60");

        CreateOrderItem corporateItem = CreateOrderItem.builder()
                .productId(corporateProduct.id())
                .quantity(1)
                .metadata(corporateMetadata)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-corporate-001",
                List.of(corporateItem)
        );

        ResponseEntity<Order> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Order createdOrder = response.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

        UUID orderId = createdOrder.id();

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order pendingApproval = orderRepository.findById(orderId).orElseThrow();
                    assertThat(pendingApproval.status()).isEqualTo(OrderStatus.PENDING_APPROVAL);
                });

        verify(orderEventPublisher, atLeastOnce()).sendOrderPendingApprovalEvent(any());
        verify(orderEventPublisher, never()).sendOrderProcessedEvent(any());
        verify(orderEventPublisher, never()).sendOrderFailedEvent(any());
    }

    @Test
    @DisplayName("Should return NOT_FOUND and not create order when at least one product does not exist")
    void shouldReturnNotFoundAndNotCreateOrderWhenOneProductDoesNotExist() {
        Product existingProduct = productRepositoryUtils.createPhysicalProduct();
        UUID nonExistentProductId = UUID.randomUUID();

        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("warehouseLocation", "SP");

        CreateOrderItem validItem = CreateOrderItem.builder()
                .productId(existingProduct.id())
                .quantity(1)
                .metadata(metadata)
                .build();

        CreateOrderItem invalidItem = CreateOrderItem.builder()
                .productId(nonExistentProductId)
                .quantity(1)
                .metadata(metadata)
                .build();

        int ordersBefore = orderRepository.findAll().size();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-mixed-valid-invalid-product",
                List.of(validItem, invalidItem)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        int ordersAfter = orderRepository.findAll().size();
        assertThat(ordersAfter).isEqualTo(ordersBefore);

        verify(orderEventPublisher, never()).sendOrderCreatedEvent(any());
    }
    
    @Test
    @DisplayName("Should return BAD_REQUEST and not create order when items list is empty")
    void shouldReturnBadRequestAndNotCreateOrderWhenItemsListIsEmpty() {
        int ordersBefore = orderRepository.findAll().size();

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-empty-items",
                List.of()
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + PORT + "/api/orders",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        int ordersAfter = orderRepository.findAll().size();
        assertThat(ordersAfter).isEqualTo(ordersBefore);

        verify(orderEventPublisher, never()).sendOrderCreatedEvent(any());
    }

}

