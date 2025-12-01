package com.loomi.order_processor.domain.order.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loomi.order_processor.domain.order.dto.OrderError;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

@DisplayName("High Value Order Handler Tests")
class HighValueOrderHandlerTest {

    private HighValueOrderHandler handler;
    private UUID testOrderId;
    private String testCustomerId;

    @BeforeEach
    void setUp() {
        handler = new HighValueOrderHandler();
        testOrderId = UUID.randomUUID();
        testCustomerId = "customer-123";
    }

    private Order createOrder(BigDecimal totalAmount) {
        OrderItem item = OrderItem.builder()
                .productId(UUID.randomUUID())
                .customerId(testCustomerId)
                .quantity(1)
                .productType(ProductType.PHYSICAL)
                .price(totalAmount)
                .metadata(new RawProductMetadata())
                .build();

        return Order.builder()
                .id(testOrderId)
                .customerId(testCustomerId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(java.util.List.of(item))
                .build();
    }

    @Nested
    @DisplayName("Validate Tests")
    class ValidateTests {

        @Test
        @DisplayName("shouldReturnInternalError_whenTotalAmountIsNull")
        void shouldReturnInternalError_whenTotalAmountIsNull() {
            Order order = Order.builder()
                    .id(testOrderId)
                    .customerId(testCustomerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(null)
                    .items(java.util.List.of())
                    .build();

            ValidationResult result = handler.validate(order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INTERNAL_ERROR.toString()));
        }

        @Test
        @DisplayName("shouldReturnOk_whenTotalAmountIsLessThanOrEqualTo10000")
        void shouldReturnOk_whenTotalAmountIsLessThanOrEqualTo10000() {
            Order order = createOrder(new BigDecimal("10000"));

            ValidationResult result = handler.validate(order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldReturnOk_whenTotalAmountIsLessThan10000")
        void shouldReturnOk_whenTotalAmountIsLessThan10000() {
            Order order = createOrder(new BigDecimal("5000"));

            ValidationResult result = handler.validate(order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldReturnOk_whenTotalAmountIsGreaterThan10000")
        void shouldReturnOk_whenTotalAmountIsGreaterThan10000() {
            Order order = createOrder(new BigDecimal("15000"));

            ValidationResult result = handler.validate(order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldReturnOk_whenTotalAmountIsMuchGreaterThan10000")
        void shouldReturnOk_whenTotalAmountIsMuchGreaterThan10000() {
            Order order = createOrder(new BigDecimal("50000"));

            ValidationResult result = handler.validate(order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }
    }

    @Nested
    @DisplayName("Process Tests")
    class ProcessTests {

        @Test
        @DisplayName("shouldReturnOk_whenProcessingOrder")
        void shouldReturnOk_whenProcessingOrder() {
            Order order = createOrder(new BigDecimal("15000"));

            OrderProcessResult result = handler.process(order);

            assertTrue(result.isProcessed());
        }

        @Test
        @DisplayName("shouldReturnOk_whenProcessingLowValueOrder")
        void shouldReturnOk_whenProcessingLowValueOrder() {
            Order order = createOrder(new BigDecimal("5000"));

            OrderProcessResult result = handler.process(order);

            assertTrue(result.isProcessed());
        }
    }
}

