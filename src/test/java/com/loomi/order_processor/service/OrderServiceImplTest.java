package com.loomi.order_processor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.app.service.OrderServiceImpl;
import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.exception.OrderNotFoundException;
import com.loomi.order_processor.domain.order.producer.OrderProducer;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.exception.HttpException;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.exception.ProductIsNotActiveException;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID testOrderId;
    private UUID testProductId;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
    }

    
    private Order createTestOrder(UUID orderId, String customerId) {
        return Order.builder()
            .id(orderId)
            .customerId(customerId)
            .build();
    }

    private Product createTestProduct(UUID productId, ProductType productType) {
        return Product.builder()
            .id(productId)
            .name("Test Product")
            .productType(productType)
            .price(BigDecimal.valueOf(10.00))
            .stockQuantity(100)
            .isActive(true)
            .metadata(new RawProductMetadata())
            .build();
    }

    private CreateOrder createTestCreateOrder(List<CreateOrderItem> items) {
        return new CreateOrder("customer-123", items);
    }

    private CreateOrderItem createTestOrderItem(UUID productId, int quantity) {
        return CreateOrderItem.builder()
            .productId(productId)
            .quantity(quantity)
            .metadata(new RawProductMetadata())
            .build();
    }

    @Test
    void shouldReturnOrder_whenOrderExists() {
        Order expectedOrder = createTestOrder(testOrderId, "customer-123");
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(expectedOrder));

        Order result = orderService.consultOrder(testOrderId);

        assertEquals(expectedOrder, result);
    }

    @Test
    void shouldThrowOrderNotFoundException_whenOrderDoesNotExist() {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.consultOrder(testOrderId);
        });
    }

    @Test
    void shouldThrowProductIsNotActiveException_whenProductIsInactive() {
        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(testProductId, 2)
        ));

        Product product = Product.builder()
            .id(testProductId)
            .name("Test Product")
            .productType(ProductType.PHYSICAL)
            .price(BigDecimal.valueOf(10.00))
            .stockQuantity(100)
            .isActive(false)
            .metadata(new RawProductMetadata())
            .build();

        when(productRepository.findAllById(List.of(testProductId))).thenReturn(List.of(product));

        assertThrows(ProductIsNotActiveException.class, () -> {
            orderService.createOrder(createOrder);
        });
    }

    @Test
    void shouldThrowProductIsNotActiveException_whenMultipleItemsHaveInactiveProducts() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(productId1, 1),
            createTestOrderItem(productId2, 3)
        ));

        Product product1 = Product.builder()
            .id(productId1)
            .name("Test Product")
            .productType(ProductType.SUBSCRIPTION)
            .price(BigDecimal.valueOf(10.00))
            .stockQuantity(100)
            .isActive(false)
            .metadata(new RawProductMetadata())
            .build();
        Product product2 = createTestProduct(productId2, ProductType.DIGITAL);

        when(productRepository.findAllById(List.of(productId1, productId2))).thenReturn(List.of(product1, product2));

        assertThrows(ProductIsNotActiveException.class, () -> {
            orderService.createOrder(createOrder);
        });
    }

    @Test
    void shouldCompleteSuccessfully_whenOrderIsCreated() {
        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(testProductId, 1)
        ));

        Product product = createTestProduct(testProductId, ProductType.PHYSICAL);
        Order savedOrder = createTestOrder(testOrderId, "customer-123");

        when(productRepository.findAllById(List.of(testProductId))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.createOrder(createOrder);

        assertEquals(testOrderId, result.id());
    }

    @Test
    void shouldCheckAllProductsAreActive_whenCreatingOrder() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(productId1, 1),
            createTestOrderItem(productId2, 2)
        ));

        Product product1 = createTestProduct(productId1, ProductType.PHYSICAL);
        Product product2 = createTestProduct(productId2, ProductType.DIGITAL);
        Order savedOrder = createTestOrder(testOrderId, "customer-123");

        when(productRepository.findAllById(List.of(productId1, productId2))).thenReturn(List.of(product1, product2));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.createOrder(createOrder);

        assertEquals(testOrderId, result.id());
        assertTrue(product1.isActive(), "Product 1 should be active");
        assertTrue(product2.isActive(), "Product 2 should be active");
    }

    @Test
    void shouldThrowHttpException_whenNoProductsFound() {
        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(testProductId, 1)
        ));

        when(productRepository.findAllById(List.of(testProductId))).thenReturn(List.of());

        assertThrows(HttpException.class, () -> {
            orderService.createOrder(createOrder);
        });
    }

    @Test
    void shouldCalculateTotalAmount_whenCreatingOrder() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();

        BigDecimal price1 = new BigDecimal("10.50");
        BigDecimal price2 = new BigDecimal("25.00");
        BigDecimal price3 = new BigDecimal("5.75");

        int quantity1 = 2;
        int quantity2 = 1;
        int quantity3 = 3;

        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(productId1, quantity1),
            createTestOrderItem(productId2, quantity2),
            createTestOrderItem(productId3, quantity3)
        ));

        Product product1 = Product.builder()
            .id(productId1)
            .name("Product 1")
            .productType(ProductType.PHYSICAL)
            .price(price1)
            .stockQuantity(100)
            .isActive(true)
            .metadata(new RawProductMetadata())
            .build();

        Product product2 = Product.builder()
            .id(productId2)
            .name("Product 2")
            .productType(ProductType.DIGITAL)
            .price(price2)
            .stockQuantity(50)
            .isActive(true)
            .metadata(new RawProductMetadata())
            .build();

        Product product3 = Product.builder()
            .id(productId3)
            .name("Product 3")
            .productType(ProductType.SUBSCRIPTION)
            .price(price3)
            .stockQuantity(200)
            .isActive(true)
            .metadata(new RawProductMetadata())
            .build();

        BigDecimal expectedTotal = price1.multiply(BigDecimal.valueOf(quantity1))
            .add(price2.multiply(BigDecimal.valueOf(quantity2)))
            .add(price3.multiply(BigDecimal.valueOf(quantity3)));

        when(productRepository.findAllById(List.of(productId1, productId2, productId3)))
            .thenReturn(List.of(product1, product2, product3));

        Order savedOrder = createTestOrder(testOrderId, "customer-123");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        Order result = orderService.createOrder(createOrder);

        assertEquals(testOrderId, result.id());
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertEquals(expectedTotal, capturedOrder.totalAmount());
    }
}
