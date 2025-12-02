package com.loomi.order_processor.domain.order.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;
import com.loomi.order_processor.domain.payment.usecase.FraudService;
import com.loomi.order_processor.domain.payment.usecase.PaymentService;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Order Handler Tests")
class PaymentOrderHandlerTest {

    @Mock
    private FraudService fraudService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentOrderHandler handler;

    private UUID testOrderId;
    private String testCustomerId;

    @BeforeEach
    void setUp() {
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
            verify(fraudService, never()).isFraud(order);
        }

        @Test
        @DisplayName("shouldReturnOk_whenFraudServiceReturnsFalse")
        void shouldReturnOk_whenFraudServiceReturnsFalse() {
            Order order = createOrder(new BigDecimal("25000"));
            when(fraudService.isFraud(order)).thenReturn(false);

            ValidationResult result = handler.validate(order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
            verify(fraudService).isFraud(order);
        }

        @Test
        @DisplayName("shouldReturnRequireHumanReview_whenFraudServiceReturnsTrue")
        void shouldReturnRequireHumanReview_whenFraudServiceReturnsTrue() {
            Order order = createOrder(new BigDecimal("25000"));
            when(fraudService.isFraud(order)).thenReturn(true);

            ValidationResult result = handler.validate(order);

            assertTrue(result.isHumanReviewRequired());
            verify(fraudService).isFraud(order);
        }

        @Test
        @DisplayName("shouldCallFraudService_whenValidatingOrder")
        void shouldCallFraudService_whenValidatingOrder() {
            Order order = createOrder(new BigDecimal("1"));
            when(fraudService.isFraud(order)).thenReturn(false);

            handler.validate(order);

            verify(fraudService).isFraud(order);
        }
    }

    @Nested
    @DisplayName("Process Tests")
    class ProcessTests {

        @Test
        @DisplayName("shouldReturnOk_whenPaymentServiceSucceeds")
        void shouldReturnOk_whenPaymentServiceSucceeds() {
            Order order = createOrder(new BigDecimal("10000"));

            OrderProcessResult result = handler.process(order);

            assertTrue(result.isProcessed());
            verify(paymentService).processOrderPayment(order);
        }

        @Test
        @DisplayName("shouldReturnFail_whenPaymentServiceThrowsException")
        void shouldReturnFail_whenPaymentServiceThrowsException() {
            Order order = createOrder(new BigDecimal("10000"));
            RuntimeException exception = new RuntimeException("Payment gateway error");
            doThrow(exception).when(paymentService).processOrderPayment(order);

            OrderProcessResult result = handler.process(order);

            assertFalse(result.isProcessed());
            assertTrue(result.getErrors().contains(OrderError.INTERNAL_ERROR.toString()));
        }
    }
}

