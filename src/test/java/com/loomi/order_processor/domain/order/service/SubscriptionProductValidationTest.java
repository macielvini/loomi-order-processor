package com.loomi.order_processor.domain.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.domain.order.dto.ItemHandlerError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Product Handler Tests")
class SubscriptionProductValidationTest {

    private UUID testProductId;
    private String testCustomerId;
    private String testGroupId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testCustomerId = "customer-123";
        testGroupId = "group-abc";
    }

    private OrderItem createOrderItem(String customerId, RawProductMetadata metadata) {
        return OrderItem.builder()
                .productId(testProductId)
                .customerId(customerId)
                .quantity(1)
                .productType(ProductType.SUBSCRIPTION)
                .price(BigDecimal.valueOf(29.90))
                .metadata(metadata)
                .build();
    }

    private Product createProduct(boolean isActive, RawProductMetadata metadata) {
        return Product.builder()
                .id(testProductId)
                .name("Test Subscription Product")
                .productType(ProductType.SUBSCRIPTION)
                .price(BigDecimal.valueOf(29.90))
                .isActive(isActive)
                .metadata(metadata)
                .build();
    }

    private RawProductMetadata createMetadataWithGroupId(String groupId) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (groupId != null) {
            metadata.put("GROUP_ID", groupId);
        }
        return metadata;
    }

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private SubscriptionProductHandler subscriptionProductHandler;

    @Test
    @DisplayName("shouldReturnInternalError_whenProductNotFound")
    void shouldReturnInternalError_whenProductNotFound() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.INTERNAL_ERROR, result.getError());
    }

    @Test
    @DisplayName("shouldReturnSubscriptionNotAvailable_whenProductIsInactive")
    void shouldReturnSubscriptionNotAvailable_whenProductIsInactive() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(false, metadata);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.SUBSCRIPTION_NOT_AVAILABLE, result.getError());
    }

    @Test
    @DisplayName("shouldReturnInternalError_whenMetadataIsNull")
    void shouldReturnInternalError_whenMetadataIsNull() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        Product product = createProduct(true, null);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.INTERNAL_ERROR, result.getError());
    }

    @Test
    @DisplayName("shouldReturnInternalError_whenGroupIdMissing")
    void shouldReturnInternalError_whenGroupIdMissing() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = new RawProductMetadata();
        Product product = createProduct(true, metadata);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.INTERNAL_ERROR, result.getError());
    }

    @Test
    @DisplayName("shouldReturnDuplicateActiveSubscription_whenSameGroupExists")
    void shouldReturnDuplicateActiveSubscription_whenSameGroupExists() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(true, metadata);
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerId(testCustomerId)
                .status(OrderStatus.PROCESSED)
                .build();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(
                testCustomerId, testGroupId))
                .thenReturn(List.of(existingOrder));

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.DUPLICATE_ACTIVE_SUBSCRIPTION, result.getError());
    }

    @Test
    @DisplayName("shouldReturnSubscriptionLimitExceeded_whenMaxSubscriptionsReached")
    void shouldReturnSubscriptionLimitExceeded_whenMaxSubscriptionsReached() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(true, metadata);
        List<Order> activeSubscriptions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            activeSubscriptions.add(Order.builder()
                    .id(UUID.randomUUID())
                    .customerId(testCustomerId)
                    .status(OrderStatus.PROCESSED)
                    .build());
        }

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(
                testCustomerId, testGroupId))
                .thenReturn(new ArrayList<>());
        when(orderRepository.findAllActiveSubscriptionsByCustomerId(testCustomerId))
                .thenReturn(activeSubscriptions);

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.SUBSCRIPTION_LIMIT_EXCEEDED, result.getError());
    }

    @Test
    @DisplayName("shouldReturnSubscriptionLimitExceeded_whenMoreThanMaxSubscriptions")
    void shouldReturnSubscriptionLimitExceeded_whenMoreThanMaxSubscriptions() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(true, metadata);
        List<Order> activeSubscriptions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            activeSubscriptions.add(Order.builder()
                    .id(UUID.randomUUID())
                    .customerId(testCustomerId)
                    .status(OrderStatus.PROCESSED)
                    .build());
        }

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(
                testCustomerId, testGroupId))
                .thenReturn(new ArrayList<>());
        when(orderRepository.findAllActiveSubscriptionsByCustomerId(testCustomerId))
                .thenReturn(activeSubscriptions);

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(ItemHandlerError.SUBSCRIPTION_LIMIT_EXCEEDED, result.getError());
    }

    @Test
    @DisplayName("shouldProcessSuccessfully_whenAllValidationsPass")
    void shouldProcessSuccessfully_whenAllValidationsPass() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(true, metadata);

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(
                testCustomerId, testGroupId))
                .thenReturn(new ArrayList<>());
        when(orderRepository.findAllActiveSubscriptionsByCustomerId(testCustomerId))
                .thenReturn(new ArrayList<>());

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("shouldProcessSuccessfully_whenDifferentGroupExists")
    void shouldProcessSuccessfully_whenDifferentGroupExists() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(true, metadata);
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerId(testCustomerId)
                .status(OrderStatus.PROCESSED)
                .build();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(
                testCustomerId, testGroupId))
                .thenReturn(new ArrayList<>());
        when(orderRepository.findAllActiveSubscriptionsByCustomerId(testCustomerId))
                .thenReturn(List.of(existingOrder));

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("shouldProcessSuccessfully_whenBelowLimit")
    void shouldProcessSuccessfully_whenBelowLimit() {
        OrderItem item = createOrderItem(testCustomerId, new RawProductMetadata());
        RawProductMetadata metadata = createMetadataWithGroupId(testGroupId);
        Product product = createProduct(true, metadata);
        List<Order> activeSubscriptions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            activeSubscriptions.add(Order.builder()
                    .id(UUID.randomUUID())
                    .customerId(testCustomerId)
                    .status(OrderStatus.PROCESSED)
                    .build());
        }

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findActiveSubscriptionsByCustomerIdAndGroupId(
                testCustomerId, testGroupId))
                .thenReturn(new ArrayList<>());
        when(orderRepository.findAllActiveSubscriptionsByCustomerId(testCustomerId))
                .thenReturn(activeSubscriptions);

        ItemHandlerResult result = subscriptionProductHandler.handle(item);

        assertTrue(result.isValid());
    }
}

