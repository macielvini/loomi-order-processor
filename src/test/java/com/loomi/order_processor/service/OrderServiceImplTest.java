package com.loomi.order_processor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.dto.ValidatorMap;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.exception.ProductNotFoundException;
import com.loomi.order_processor.domain.product.exception.ProductValidationException;
import com.loomi.order_processor.domain.product.repository.ProductRepository;
import com.loomi.order_processor.domain.product.service.ProductValidator;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ValidatorMap validatorMap;

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

    private ProductValidator createMockValidator(ValidationResult result) {
        ProductValidator validator = org.mockito.Mockito.mock(ProductValidator.class);
        when(validator.validate(any(Product.class))).thenReturn(result);
        return validator;
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
    void shouldThrowProductValidationException_whenValidationFails() {
        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(testProductId, 2)
        ));

        Product product = createTestProduct(testProductId, ProductType.PHYSICAL);
        ProductValidator validator = createMockValidator(ValidationResult.fail("Product out of stock"));

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(validatorMap.getValidatorsFor(product)).thenReturn(List.of(validator));

        assertThrows(ProductValidationException.class, () -> {
            orderService.createOrder(createOrder);
        });
    }

    @Test
    void shouldThrowProductValidationException_whenMultipleItemsHaveValidationErrors() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(productId1, 1),
            createTestOrderItem(productId2, 3)
        ));

        Product product1 = createTestProduct(productId1, ProductType.SUBSCRIPTION);
        Product product2 = createTestProduct(productId2, ProductType.DIGITAL);

        ProductValidator validator1 = createMockValidator(ValidationResult.fail("Invalid subscription period"));
        ProductValidator validator2 = createMockValidator(ValidationResult.fail("License expired"));

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(validatorMap.getValidatorsFor(product1)).thenReturn(List.of(validator1));
        when(validatorMap.getValidatorsFor(product2)).thenReturn(List.of(validator2));

        assertThrows(ProductValidationException.class, () -> {
            orderService.createOrder(createOrder);
        });
    }

    @Test
    void shouldCompleteSuccessfully_whenValidationPasses() {
        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(testProductId, 1)
        ));

        Product product = createTestProduct(testProductId, ProductType.PHYSICAL);
        ProductValidator validator = createMockValidator(ValidationResult.ok());
        Order savedOrder = createTestOrder(testOrderId, "customer-123");

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(productRepository.findAllById(List.of(testProductId))).thenReturn(List.of(product));
        when(validatorMap.getValidatorsFor(product)).thenReturn(List.of(validator));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UUID result = orderService.createOrder(createOrder);

        assertEquals(testOrderId, result);
    }

    @Test
    void shouldValidateAllItems_whenCreatingOrder() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(productId1, 1),
            createTestOrderItem(productId2, 2)
        ));

        Product product1 = createTestProduct(productId1, ProductType.PHYSICAL);
        Product product2 = createTestProduct(productId2, ProductType.DIGITAL);

        ProductValidator validator1 = createMockValidator(ValidationResult.ok());
        ProductValidator validator2 = createMockValidator(ValidationResult.ok());
        Order savedOrder = createTestOrder(testOrderId, "customer-123");

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findAllById(List.of(productId1, productId2))).thenReturn(List.of(product1, product2));
        when(validatorMap.getValidatorsFor(product1)).thenReturn(List.of(validator1));
        when(validatorMap.getValidatorsFor(product2)).thenReturn(List.of(validator2));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UUID result = orderService.createOrder(createOrder);

        assertEquals(testOrderId, result);
        verify(validator1, times(1)).validate(product1);
        verify(validator2, times(1)).validate(product2);
        
        ValidationResult result1 = validator1.validate(product1);
        ValidationResult result2 = validator2.validate(product2);
        
        assertTrue(result1.isValid(), "Validator 1 should return valid result");
        assertTrue(result2.isValid(), "Validator 2 should return valid result");
    }

    @Test
    void shouldThrowProductNotFoundException_whenProductNotFound() {
        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(testProductId, 1)
        ));

        assertThrows(ProductNotFoundException.class, () -> {
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

        ProductValidator validator1 = createMockValidator(ValidationResult.ok());
        ProductValidator validator2 = createMockValidator(ValidationResult.ok());
        ProductValidator validator3 = createMockValidator(ValidationResult.ok());

        BigDecimal expectedTotal = price1.multiply(BigDecimal.valueOf(quantity1))
            .add(price2.multiply(BigDecimal.valueOf(quantity2)))
            .add(price3.multiply(BigDecimal.valueOf(quantity3)));

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findById(productId3)).thenReturn(Optional.of(product3));
        when(productRepository.findAllById(List.of(productId1, productId2, productId3)))
            .thenReturn(List.of(product1, product2, product3));
        when(validatorMap.getValidatorsFor(product1)).thenReturn(List.of(validator1));
        when(validatorMap.getValidatorsFor(product2)).thenReturn(List.of(validator2));
        when(validatorMap.getValidatorsFor(product3)).thenReturn(List.of(validator3));

        Order savedOrder = createTestOrder(testOrderId, "customer-123");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        UUID result = orderService.createOrder(createOrder);

        assertEquals(testOrderId, result);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertEquals(expectedTotal, capturedOrder.totalAmount());
    }
}
