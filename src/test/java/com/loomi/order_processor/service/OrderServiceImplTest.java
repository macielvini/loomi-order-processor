package com.loomi.order_processor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.loomi.order_processor.domain.order.dto.OrderItem;
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

    private CreateOrder createTestCreateOrder(List<OrderItem> items) {
        return new CreateOrder("customer-123", items);
    }

    private OrderItem createTestOrderItem(UUID productId, int quantity, BigDecimal price) {
        return OrderItem.builder()
            .productId(productId)
            .quantity(quantity)
            .price(price)
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
            createTestOrderItem(testProductId, 2, BigDecimal.valueOf(10.00))
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
            createTestOrderItem(productId1, 1, BigDecimal.valueOf(20.00)),
            createTestOrderItem(productId2, 3, BigDecimal.valueOf(15.00))
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
            createTestOrderItem(testProductId, 1, BigDecimal.valueOf(25.00))
        ));

        Product product = createTestProduct(testProductId, ProductType.PHYSICAL);
        ProductValidator validator = createMockValidator(ValidationResult.ok());

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        when(validatorMap.getValidatorsFor(product)).thenReturn(List.of(validator));

        assertDoesNotThrow(() -> orderService.createOrder(createOrder));
    }

    @Test
    void shouldValidateAllItems_whenCreatingOrder() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        CreateOrder createOrder = createTestCreateOrder(List.of(
            createTestOrderItem(productId1, 1, BigDecimal.valueOf(10.00)),
            createTestOrderItem(productId2, 2, BigDecimal.valueOf(20.00))
        ));

        Product product1 = createTestProduct(productId1, ProductType.PHYSICAL);
        Product product2 = createTestProduct(productId2, ProductType.DIGITAL);

        ProductValidator validator1 = createMockValidator(ValidationResult.ok());
        ProductValidator validator2 = createMockValidator(ValidationResult.ok());

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(validatorMap.getValidatorsFor(product1)).thenReturn(List.of(validator1));
        when(validatorMap.getValidatorsFor(product2)).thenReturn(List.of(validator2));

        assertDoesNotThrow(() -> orderService.createOrder(createOrder));

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
            createTestOrderItem(testProductId, 1, BigDecimal.valueOf(25.00))
        ));

        assertThrows(ProductNotFoundException.class, () -> {
            orderService.createOrder(createOrder);
        });
    }
}
