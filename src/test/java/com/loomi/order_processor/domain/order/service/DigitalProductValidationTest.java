package com.loomi.order_processor.domain.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.domain.notification.service.EmailService;
import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Digital Product Handler Tests")
class DigitalProductValidationTest {

    private UUID testProductId;
    private UUID testOrderId;
    private String testCustomerId;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DigitalItemHandler digitalItemHandler;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testCustomerId = "customer-123";
    }

    private OrderItem createOrderItem(int quantity, String customerId, RawProductMetadata metadata) {
        return OrderItem.builder()
                .productId(testProductId)
                .customerId(customerId)
                .quantity(quantity)
                .productType(ProductType.DIGITAL)
                .price(BigDecimal.valueOf(39.90))
                .metadata(metadata)
                .build();
    }

    private Product createProduct(Integer stockQuantity, boolean isActive) {
        return Product.builder()
                .id(testProductId)
                .name("Test Digital Product")
                .productType(ProductType.DIGITAL)
                .price(BigDecimal.valueOf(39.90))
                .stockQuantity(stockQuantity)
                .isActive(isActive)
                .metadata(new RawProductMetadata())
                .build();
    }

    private RawProductMetadata createMetadata(String deliveryEmail) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (deliveryEmail != null) {
            metadata.put("deliveryEmail", deliveryEmail);
        }
        return metadata;
    }

    private Order createOrder(OrderItem item) {
        return Order.builder()
                .id(testOrderId)
                .customerId(testCustomerId)
                .items(java.util.List.of(item))
                .build();
    }

    @Test
    @DisplayName("shouldSupportOnlyDigitalProductType")
    void shouldSupportOnlyDigitalProductType() {
        assertEquals(ProductType.DIGITAL, digitalItemHandler.supportedType());
    }

    @Nested
    @DisplayName("Validate Tests")
    class ValidateTests {

        @Test
        @DisplayName("shouldReturnDistributionRightsExpired_whenProductIsInactive")
        void shouldReturnDistributionRightsExpired_whenProductIsInactive() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(100, false);
            Order order = createOrder(item);

            ValidationResult result = digitalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.DISTRIBUTION_RIGHTS_EXPIRED.toString()));
            verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("shouldReturnLicenseUnavailable_whenStockQuantityIsNull")
        void shouldReturnLicenseUnavailable_whenStockQuantityIsNull() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(null, true);
            Order order = createOrder(item);

            ValidationResult result = digitalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.LICENSE_UNAVAILABLE.toString()));
            verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("shouldReturnLicenseUnavailable_whenNoLicensesAvailable")
        void shouldReturnLicenseUnavailable_whenNoLicensesAvailable() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(0, true);
            Order order = createOrder(item);

            ValidationResult result = digitalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.LICENSE_UNAVAILABLE.toString()));
            verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("shouldReturnAlreadyOwned_whenCustomerAlreadyHasProduct")
        void shouldReturnAlreadyOwned_whenCustomerAlreadyHasProduct() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(100, true);
            Order order = createOrder(item);
            Order existingOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .customerId(testCustomerId)
                    .status(OrderStatus.PROCESSED)
                    .build();

            when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                    testCustomerId, testProductId, OrderStatus.PROCESSED))
                    .thenReturn(List.of(existingOrder));

            ValidationResult result = digitalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.ALREADY_OWNED.toString()));
            verify(orderRepository).findByCustomerIdAndProductIdAndStatus(
                    testCustomerId, testProductId, OrderStatus.PROCESSED);
        }

        @Test
        @DisplayName("shouldReturnOk_whenAllValidationsPass")
        void shouldReturnOk_whenAllValidationsPass() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                    testCustomerId, testProductId, OrderStatus.PROCESSED))
                    .thenReturn(new ArrayList<>());

            ValidationResult result = digitalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
            verify(orderRepository).findByCustomerIdAndProductIdAndStatus(
                    testCustomerId, testProductId, OrderStatus.PROCESSED);
        }
    }

    @Nested
    @DisplayName("Process Tests")
    class ProcessTests {

        @Test
        @DisplayName("shouldReserveLicenseAndSendEmailSuccessfully_whenAllValidationsPass")
        void shouldReserveLicenseAndSendEmailSuccessfully_whenAllValidationsPass() {
            String deliveryEmail = "customer@example.com";
            OrderItem item = createOrderItem(1, testCustomerId, createMetadata(deliveryEmail));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = digitalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(99, updatedProduct.stockQuantity());

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(emailService).sendTo(emailCaptor.capture(), payloadCaptor.capture());

            assertEquals(deliveryEmail, emailCaptor.getValue());
            assertNotNull(payloadCaptor.getValue());
        }

        @Test
        @DisplayName("shouldUseDefaultEmail_whenDeliveryEmailNotInMetadata")
        void shouldUseDefaultEmail_whenDeliveryEmailNotInMetadata() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = digitalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendTo(emailCaptor.capture(), any());
            assertEquals("customer@example.com", emailCaptor.getValue());
        }

        @Test
        @DisplayName("shouldReserveOnlyOneLicense_whenQuantityIsGreaterThanOne")
        void shouldReserveOnlyOneLicense_whenQuantityIsGreaterThanOne() {
            OrderItem item = createOrderItem(5, testCustomerId, new RawProductMetadata());
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = digitalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            // Should reserve only 1 license even if quantity is 5
            assertEquals(99, updatedProduct.stockQuantity());
        }

        @Test
        @DisplayName("shouldProcessSuccessfully_whenNoExistingOrders")
        void shouldProcessSuccessfully_whenNoExistingOrders() {
            OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = digitalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            verify(productRepository).update(any());
            verify(emailService).sendTo(any(), any());
        }
    }
}
