package com.loomi.order_processor.domain.order.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.loomi.order_processor.app.service.order.handler.item.PreOrderItemHandler;
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
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pre-Order Product Handler Tests")
class PreOrderItemHandlerTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private UUID testProductId;
    private UUID testOrderId;
    private String testCustomerId;

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private PreOrderItemHandler preOrderItemHandler;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testCustomerId = "customer-123";
    }

    private OrderItem createOrderItem(int quantity, BigDecimal price, RawProductMetadata metadata) {
        return OrderItem.builder()
                .productId(testProductId)
                .customerId(testCustomerId)
                .quantity(quantity)
                .productType(ProductType.PRE_ORDER)
                .price(price)
                .metadata(metadata)
                .build();
    }

    private Product createProduct(boolean isActive, Integer stockQuantity, RawProductMetadata metadata) {
        return Product.builder()
                .id(testProductId)
                .name("Test Pre-Order Product")
                .productType(ProductType.PRE_ORDER)
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(stockQuantity)
                .isActive(isActive)
                .metadata(metadata)
                .build();
    }

    private RawProductMetadata createMetadataWithReleaseDate(String releaseDate) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (releaseDate != null) {
            metadata.put("releaseDate", releaseDate);
        }
        return metadata;
    }

    private RawProductMetadata createItemMetadata(String warehouseLocation) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (warehouseLocation != null) {
            metadata.put("warehouseLocation", warehouseLocation);
        }
        return metadata;
    }

    private RawProductMetadata createProductMetadataWithDiscount(String releaseDate, String discount) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (releaseDate != null) {
            metadata.put("releaseDate", releaseDate);
        }
        if (discount != null) {
            metadata.put("preOrderDiscount", discount);
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
    @DisplayName("shouldSupportOnlyPreOrderProductType")
    void shouldSupportOnlyPreOrderProductType() {
        assertEquals(ProductType.PRE_ORDER, preOrderItemHandler.supportedType());
    }

    @Nested
    @DisplayName("Validate Tests")
    class ValidateTests {

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenProductIsInactive")
        void shouldReturnInvalidReleaseDate_whenProductIsInactive() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(false, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenProductMetadataIsNull")
        void shouldReturnInvalidReleaseDate_whenProductMetadataIsNull() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, null);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenReleaseDateIsMissing")
        void shouldReturnInvalidReleaseDate_whenReleaseDateIsMissing() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, new RawProductMetadata());
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenReleaseDateIsEmpty")
        void shouldReturnInvalidReleaseDate_whenReleaseDateIsEmpty() {
            RawProductMetadata productMetadata = createMetadataWithReleaseDate("");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenReleaseDateIsBlank")
        void shouldReturnInvalidReleaseDate_whenReleaseDateIsBlank() {
            RawProductMetadata productMetadata = createMetadataWithReleaseDate("   ");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenReleaseDateHasInvalidFormat")
        void shouldReturnInvalidReleaseDate_whenReleaseDateHasInvalidFormat() {
            RawProductMetadata productMetadata = createMetadataWithReleaseDate("invalid-date");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnReleaseDatePassed_whenReleaseDateIsToday")
        void shouldReturnReleaseDatePassed_whenReleaseDateIsToday() {
            LocalDate today = LocalDate.now();
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(today.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.RELEASE_DATE_PASSED.toString()));
        }

        @Test
        @DisplayName("shouldReturnReleaseDatePassed_whenReleaseDateIsInPast")
        void shouldReturnReleaseDatePassed_whenReleaseDateIsInPast() {
            LocalDate pastDate = LocalDate.now().minusDays(10);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(pastDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.RELEASE_DATE_PASSED.toString()));
        }

        @Test
        @DisplayName("shouldReturnPreOrderSoldOut_whenStockQuantityIsNull")
        void shouldReturnPreOrderSoldOut_whenStockQuantityIsNull() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, null, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.PRE_ORDER_SOLD_OUT.toString()));
        }

        @Test
        @DisplayName("shouldReturnPreOrderSoldOut_whenStockQuantityIsInsufficient")
        void shouldReturnPreOrderSoldOut_whenStockQuantityIsInsufficient() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(10, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 5, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.PRE_ORDER_SOLD_OUT.toString()));
        }

        @Test
        @DisplayName("shouldReturnOk_whenAllValidationsPass")
        void shouldReturnOk_whenAllValidationsPass() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(5, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            ValidationResult result = preOrderItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("Process Tests")
    class ProcessTests {

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenReleaseDateIsMissingInProcess")
        void shouldReturnInvalidReleaseDate_whenReleaseDateIsMissingInProcess() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, new RawProductMetadata());
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertFalse(result.isProcessed());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidReleaseDate_whenReleaseDateHasInvalidFormat")
        void shouldReturnInvalidReleaseDate_whenReleaseDateHasInvalidFormat() {
            RawProductMetadata productMetadata = createMetadataWithReleaseDate("invalid-date");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertFalse(result.isProcessed());
            assertTrue(result.getErrors().contains(OrderError.INVALID_RELEASE_DATE.toString()));
        }

        @Test
        @DisplayName("shouldProcessSuccessfully_andAddReleaseDateAndMaxCancellationDate")
        void shouldProcessSuccessfully_andAddReleaseDateAndMaxCancellationDate() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertNotNull(item.metadata());
            assertEquals(futureDate.format(DATE_FORMATTER), item.metadata().get("releaseDate"));
            
            LocalDate expectedMaxCancellationDate = futureDate.minusDays(7);
            assertEquals(expectedMaxCancellationDate.format(DATE_FORMATTER), item.metadata().get("maxCancellationDate"));
        }

        @Test
        @DisplayName("shouldInitializeMetadata_whenItemMetadataIsNull")
        void shouldInitializeMetadata_whenItemMetadataIsNull() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertNotNull(item.metadata());
        }

        @Test
        @DisplayName("shouldCalculateDeliveryDays_whenWarehouseLocationExists")
        void shouldCalculateDeliveryDays_whenWarehouseLocationExists() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            RawProductMetadata itemMetadata = createItemMetadata("SP");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), itemMetadata);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            when(deliveryService.calculateDeliveryDays("SP")).thenReturn(5);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(5, item.metadata().get("deliveryDays"));
        }

        @Test
        @DisplayName("shouldNotCalculateDeliveryDays_whenWarehouseLocationDoesNotExist")
        void shouldNotCalculateDeliveryDays_whenWarehouseLocationDoesNotExist() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), new RawProductMetadata());
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertFalse(item.metadata().containsKey("deliveryDays"));
        }

        @Test
        @DisplayName("shouldApplyPreOrderDiscount_whenDiscountExists")
        void shouldApplyPreOrderDiscount_whenDiscountExists() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createProductMetadataWithDiscount(
                    futureDate.format(DATE_FORMATTER), "10.50");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(new BigDecimal("89.50"), item.price());
        }

        @Test
        @DisplayName("shouldNotApplyDiscount_whenPreOrderDiscountDoesNotExist")
        void shouldNotApplyDiscount_whenPreOrderDiscountDoesNotExist() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            BigDecimal originalPrice = BigDecimal.valueOf(100.00);
            OrderItem item = createOrderItem(1, originalPrice, null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(originalPrice, item.price());
        }

        @Test
        @DisplayName("shouldApplyDiscountWithWhitespace_whenDiscountHasSpaces")
        void shouldApplyDiscountWithWhitespace_whenDiscountHasSpaces() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createProductMetadataWithDiscount(
                    futureDate.format(DATE_FORMATTER), "  15.25  ");
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(new BigDecimal("84.75"), item.price());
        }

        @Test
        @DisplayName("shouldCalculateMaxCancellationDateCorrectly")
        void shouldCalculateMaxCancellationDateCorrectly() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            RawProductMetadata productMetadata = createMetadataWithReleaseDate(futureDate.format(DATE_FORMATTER));
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(100.00), null);
            Product product = createProduct(true, 100, productMetadata);
            Order order = createOrder(item);

            OrderProcessResult result = preOrderItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            LocalDate expectedMaxCancellationDate = futureDate.minusDays(7);
            assertEquals(expectedMaxCancellationDate.format(DATE_FORMATTER), item.metadata().get("maxCancellationDate"));
        }
    }
}

