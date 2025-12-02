package com.loomi.order_processor.infra.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.loomi.order_processor.TestcontainersConfiguration;
import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;
import com.loomi.order_processor.infra.web.dto.CreateOrderRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderEndToEndIntegrationTest {

    @LocalServerPort
    private int PORT;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoSpyBean
    private OrderProducer orderProducer;

    private Product createPhysicalProduct() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("warehouseLocation", "SP");

        Product product = Product.builder()
                .name("Physical Product")
                .productType(ProductType.PHYSICAL)
                .price(new BigDecimal("199.90"))
                .stockQuantity(100)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    private Product createSubscriptionProductForMixedOrder() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("GROUP_ID", "SUB_GROUP_MIXED_1");

        Product product = Product.builder()
                .name("Subscription Product")
                .productType(ProductType.SUBSCRIPTION)
                .price(new BigDecimal("49.90"))
                .stockQuantity(0)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    private Product createDigitalProductForMixedOrder() {
        RawProductMetadata metadata = new RawProductMetadata();

        Product product = Product.builder()
                .name("Digital Product")
                .productType(ProductType.DIGITAL)
                .price(new BigDecimal("29.90"))
                .stockQuantity(10)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    private Product createPreOrderProductForMixedOrder() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("releaseDate", LocalDate.now().plusDays(30).toString());

        Product product = Product.builder()
                .name("Pre-order Product")
                .productType(ProductType.PRE_ORDER)
                .price(new BigDecimal("199.00"))
                .stockQuantity(50)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    private Product createHighValuePhysicalProduct() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("warehouseLocation", "SP");

        Product product = Product.builder()
                .name("High Value Physical Product")
                .productType(ProductType.PHYSICAL)
                .price(new BigDecimal("15000.00"))
                .stockQuantity(100)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    @Test
    @DisplayName("Should process a physical order end-to-end from API to PROCESSED status")
    void shouldProcessPhysicalOrderEndToEnd() {
        Product product = createPhysicalProduct();

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

        verify(orderProducer, atLeastOnce()).sendOrderProcessedEvent(any());
    }

    @Test
    @DisplayName("Should mark order as FAILED and publish OrderFailedEvent when at least one item fails")
    void shouldMarkOrderAsFailedWhenAtLeastOneItemFailsEndToEnd() {
        Product okProduct = createPhysicalProduct();
        Product failingProduct = createPhysicalProduct();
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

        verify(orderProducer, atLeastOnce()).sendOrderFailedEvent(any());
    }

    @Test
    @DisplayName("Should process high value physical order without fraud end-to-end")
    void shouldProcessHighValuePhysicalOrderWithoutFraudEndToEnd() {
        Product product = createHighValuePhysicalProduct();

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

        verify(orderProducer, atLeastOnce()).sendOrderProcessedEvent(any());
        verify(orderProducer, never()).sendOrderFailedEvent(any());
        verify(orderProducer, never()).sendOrderPendingApprovalEvent(any());
    }

    @Test
    @DisplayName("Should process a mixed order with multiple product types end-to-end")
    void shouldProcessMixedOrderWithMultipleProductTypesEndToEnd() {
        Product physical = createPhysicalProduct();
        Product subscription = createSubscriptionProductForMixedOrder();
        Product digital = createDigitalProductForMixedOrder();
        Product preOrder = createPreOrderProductForMixedOrder();

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

        verify(orderProducer, atLeastOnce()).sendOrderProcessedEvent(any());
    }

}

