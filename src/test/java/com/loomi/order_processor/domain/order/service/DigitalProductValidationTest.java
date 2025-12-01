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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.domain.notification.service.EmailService;
import com.loomi.order_processor.domain.order.dto.OrderError;
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
@DisplayName("Digital Product Handler Tests")
class DigitalProductValidationTest {

    private UUID testProductId;
    private String testCustomerId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
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

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DigitalProductHandler digitalProductHandler;

    @Test
    @DisplayName("shouldReturnInternalError_whenProductNotFound")
    void shouldReturnInternalError_whenProductNotFound() {
        OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
        when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(OrderError.INTERNAL_ERROR, result.getError());
        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).update(any());
        verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        verify(emailService, never()).sendTo(any(), any());
    }

    @Test
    @DisplayName("shouldReturnDistributionRightsExpired_whenProductIsInactive")
    void shouldReturnDistributionRightsExpired_whenProductIsInactive() {
        OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
        Product product = createProduct(100, false);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(OrderError.DISTRIBUTION_RIGHTS_EXPIRED, result.getError());
        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).update(any());
        verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        verify(emailService, never()).sendTo(any(), any());
    }

    @Test
    @DisplayName("shouldReturnLicenseUnavailable_whenStockQuantityIsNull")
    void shouldReturnLicenseUnavailable_whenStockQuantityIsNull() {
        OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
        Product product = createProduct(null, true);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(OrderError.LICENSE_UNAVAILABLE, result.getError());
        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).update(any());
        verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        verify(emailService, never()).sendTo(any(), any());
    }

    @Test
    @DisplayName("shouldReturnLicenseUnavailable_whenNoLicensesAvailable")
    void shouldReturnLicenseUnavailable_whenNoLicensesAvailable() {
        OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
        Product product = createProduct(0, true);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(OrderError.LICENSE_UNAVAILABLE, result.getError());
        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).update(any());
        verify(orderRepository, never()).findByCustomerIdAndProductIdAndStatus(any(), any(), any());
        verify(emailService, never()).sendTo(any(), any());
    }

    @Test
    @DisplayName("shouldReturnAlreadyOwned_whenCustomerAlreadyHasProduct")
    void shouldReturnAlreadyOwned_whenCustomerAlreadyHasProduct() {
        OrderItem item = createOrderItem(1, testCustomerId, new RawProductMetadata());
        Product product = createProduct(100, true);
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerId(testCustomerId)
                .status(OrderStatus.PROCESSED)
                .build();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED))
                .thenReturn(List.of(existingOrder));

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertFalse(result.isValid());
        assertEquals(OrderError.ALREADY_OWNED, result.getError());
        verify(productRepository).findById(testProductId);
        verify(orderRepository).findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED);
        verify(productRepository, never()).update(any());
        verify(emailService, never()).sendTo(any(), any());
    }

    @Test
    @DisplayName("shouldReserveLicenseAndSendEmailSuccessfully_whenAllValidationsPass")
    void shouldReserveLicenseAndSendEmailSuccessfully_whenAllValidationsPass() {
        String deliveryEmail = "customer@example.com";
        OrderItem item = createOrderItem(1, testCustomerId, createMetadata(deliveryEmail));
        Product product = createProduct(100, true);

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED))
                .thenReturn(new ArrayList<>());

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertTrue(result.isValid());

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

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED))
                .thenReturn(new ArrayList<>());

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertTrue(result.isValid());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendTo(emailCaptor.capture(), any());
        assertEquals("customer@example.com", emailCaptor.getValue());
    }

    @Test
    @DisplayName("shouldReserveOnlyOneLicense_whenQuantityIsGreaterThanOne")
    void shouldReserveOnlyOneLicense_whenQuantityIsGreaterThanOne() {
        OrderItem item = createOrderItem(5, testCustomerId, new RawProductMetadata());
        Product product = createProduct(100, true);

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED))
                .thenReturn(new ArrayList<>());

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertTrue(result.isValid());

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

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED))
                .thenReturn(new ArrayList<>());

        ItemHandlerResult result = digitalProductHandler.handle(item);

        assertTrue(result.isValid());
        verify(orderRepository).findByCustomerIdAndProductIdAndStatus(
                testCustomerId, testProductId, OrderStatus.PROCESSED);
    }
}
