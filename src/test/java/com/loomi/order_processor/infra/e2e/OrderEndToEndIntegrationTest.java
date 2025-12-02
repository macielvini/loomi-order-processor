package com.loomi.order_processor.infra.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
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
}

