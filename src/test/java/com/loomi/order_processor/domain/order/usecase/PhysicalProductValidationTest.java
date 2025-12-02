package com.loomi.order_processor.domain.order.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.UUID;

import com.loomi.order_processor.app.service.order.handler.PhysicalItemHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.event.usecase.AlertEventPublisher;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Physical Product Handler Tests")
class PhysicalProductValidationTest {

    private UUID testProductId;
    private UUID testOrderId;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AlertEventPublisher alertProducer;

    private DeliveryService deliveryService;

    @InjectMocks
    private PhysicalItemHandler physicalItemHandler;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        deliveryService = new DeliveryService();
        physicalItemHandler = new PhysicalItemHandler(productRepository, alertProducer, deliveryService);
    }

    private OrderItem createOrderItem(int quantity, RawProductMetadata metadata) {
        return OrderItem.builder()
                .productId(testProductId)
                .quantity(quantity)
                .productType(ProductType.PHYSICAL)
                .price(BigDecimal.valueOf(100.00))
                .metadata(metadata)
                .build();
    }

    private Product createProduct(Integer stockQuantity, boolean isActive) {
        return Product.builder()
                .id(testProductId)
                .name("Test Physical Product")
                .productType(ProductType.PHYSICAL)
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(stockQuantity)
                .isActive(isActive)
                .metadata(new RawProductMetadata())
                .build();
    }

    private RawProductMetadata createMetadata(String warehouseLocation) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (warehouseLocation != null) {
            metadata.put("warehouseLocation", warehouseLocation);
        }
        return metadata;
    }

    private Order createOrder(OrderItem item) {
        return Order.builder()
                .id(testOrderId)
                .customerId("customer-123")
                .items(java.util.List.of(item))
                .build();
    }

    @Test
    @DisplayName("shouldSupportOnlyPhysicalProductType")
    void shouldSupportOnlyPhysicalProductType() {
        assertEquals(ProductType.PHYSICAL, physicalItemHandler.supportedType());
    }

    @Nested
    @DisplayName("Validate Tests")
    class ValidateTests {

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenMetadataIsNull")
        void shouldReturnWarehouseUnavailable_whenMetadataIsNull() {
            OrderItem item = createOrderItem(1, null);
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.WAREHOUSE_UNAVAILABLE.toString()));
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenWarehouseLocationIsMissing")
        void shouldReturnWarehouseUnavailable_whenWarehouseLocationIsMissing() {
            OrderItem item = createOrderItem(1, new RawProductMetadata());
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.WAREHOUSE_UNAVAILABLE.toString()));
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenWarehouseLocationIsEmpty")
        void shouldReturnWarehouseUnavailable_whenWarehouseLocationIsEmpty() {
            OrderItem item = createOrderItem(1, createMetadata(""));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.WAREHOUSE_UNAVAILABLE.toString()));
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenWarehouseLocationIsWhitespace")
        void shouldReturnWarehouseUnavailable_whenWarehouseLocationIsWhitespace() {
            OrderItem item = createOrderItem(1, createMetadata("   "));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.WAREHOUSE_UNAVAILABLE.toString()));
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenLocationIsInvalid")
        void shouldReturnWarehouseUnavailable_whenLocationIsInvalid() {
            OrderItem item = createOrderItem(1, createMetadata("INVALID"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.WAREHOUSE_UNAVAILABLE.toString()));
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsSP")
        void shouldReturnOk_whenLocationIsSP() {
            OrderItem item = createOrderItem(1, createMetadata("SP"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsRJ")
        void shouldReturnOk_whenLocationIsRJ() {
            OrderItem item = createOrderItem(1, createMetadata("RJ"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsMG")
        void shouldReturnOk_whenLocationIsMG() {
            OrderItem item = createOrderItem(1, createMetadata("MG"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldHandleWhitespace_whenLocationHasSpaces")
        void shouldHandleWhitespace_whenLocationHasSpaces() {
            OrderItem item = createOrderItem(1, createMetadata("  SP  "));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsCaseInsensitive")
        void shouldReturnOk_whenLocationIsCaseInsensitive() {
            OrderItem item = createOrderItem(1, createMetadata("sp"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOutOfStock_whenProductIsInactive")
        void shouldReturnOutOfStock_whenProductIsInactive() {
            OrderItem item = createOrderItem(2, createMetadata("SP"));
            Product product = createProduct(100, false);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.OUT_OF_STOCK.toString()));
        }

        @Test
        @DisplayName("shouldReturnOutOfStock_whenStockQuantityIsNull")
        void shouldReturnOutOfStock_whenStockQuantityIsNull() {
            OrderItem item = createOrderItem(2, createMetadata("SP"));
            Product product = createProduct(null, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.OUT_OF_STOCK.toString()));
        }

        @Test
        @DisplayName("shouldReturnOutOfStock_whenStockQuantityIsLessThanRequired")
        void shouldReturnOutOfStock_whenStockQuantityIsLessThanRequired() {
            OrderItem item = createOrderItem(10, createMetadata("SP"));
            Product product = createProduct(5, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.OUT_OF_STOCK.toString()));
        }

        @Test
        @DisplayName("shouldReturnOk_whenAllValidationsPass")
        void shouldReturnOk_whenAllValidationsPass() {
            OrderItem item = createOrderItem(5, createMetadata("SP"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            ValidationResult result = physicalItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("Process Tests")
    class ProcessTests {

        @Test
        @DisplayName("shouldUpdateStockSuccessfully_whenStockIsSufficient")
        void shouldUpdateStockSuccessfully_whenStockIsSufficient() {
            OrderItem item = createOrderItem(5, createMetadata("SP"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(95, updatedProduct.stockQuantity());
            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldSendLowStockAlert_whenRemainingStockIsBelowThreshold")
        void shouldSendLowStockAlert_whenRemainingStockIsBelowThreshold() {
            OrderItem item = createOrderItem(10, createMetadata("SP"));
            Product product = createProduct(12, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(2, updatedProduct.stockQuantity());

            ArgumentCaptor<LowStockAlertEvent> alertCaptor = ArgumentCaptor.forClass(LowStockAlertEvent.class);
            verify(alertProducer).sendLowStockAlert(alertCaptor.capture());
            LowStockAlertEvent alertEvent = alertCaptor.getValue();
            assertEquals(testProductId, alertEvent.getPayload().getProductId());
            assertEquals(2, alertEvent.getPayload().getCurrentStock());
            assertEquals(5, alertEvent.getPayload().getThreshold());
        }

        @Test
        @DisplayName("shouldNotSendLowStockAlert_whenRemainingStockEqualsThreshold")
        void shouldNotSendLowStockAlert_whenRemainingStockEqualsThreshold() {
            OrderItem item = createOrderItem(5, createMetadata("SP"));
            Product product = createProduct(10, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(5, updatedProduct.stockQuantity());

            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldNotSendLowStockAlert_whenRemainingStockIsAboveThreshold")
        void shouldNotSendLowStockAlert_whenRemainingStockIsAboveThreshold() {
            OrderItem item = createOrderItem(5, createMetadata("SP"));
            Product product = createProduct(20, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(15, updatedProduct.stockQuantity());

            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldCalculate5Days_whenLocationIsSP")
        void shouldCalculate5Days_whenLocationIsSP() {
            OrderItem item = createOrderItem(1, createMetadata("SP"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(5, item.metadata().get("deliveryDays"));
        }

        @Test
        @DisplayName("shouldCalculate7Days_whenLocationIsRJ")
        void shouldCalculate7Days_whenLocationIsRJ() {
            OrderItem item = createOrderItem(1, createMetadata("RJ"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(7, item.metadata().get("deliveryDays"));
        }

        @Test
        @DisplayName("shouldCalculate10Days_whenLocationIsMG")
        void shouldCalculate10Days_whenLocationIsMG() {
            OrderItem item = createOrderItem(1, createMetadata("MG"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(10, item.metadata().get("deliveryDays"));
        }

        @Test
        @DisplayName("shouldUseDefaultDeliveryTime_whenLocationIsUnknown")
        void shouldUseDefaultDeliveryTime_whenLocationIsUnknown() {
            OrderItem item = createOrderItem(1, createMetadata("UNKNOWN"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(10, item.metadata().get("deliveryDays"));
        }

        @Test
        @DisplayName("shouldHandleCaseInsensitivity_whenLocationIsLowerCase")
        void shouldHandleCaseInsensitivity_whenLocationIsLowerCase() {
            OrderItem item = createOrderItem(1, createMetadata("sp"));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(5, item.metadata().get("deliveryDays"));
        }

        @Test
        @DisplayName("shouldHandleWhitespace_whenLocationHasSpaces")
        void shouldHandleWhitespace_whenLocationHasSpaces() {
            OrderItem item = createOrderItem(1, createMetadata("  SP  "));
            Product product = createProduct(100, true);
            Order order = createOrder(item);

            OrderProcessResult result = physicalItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertEquals(5, item.metadata().get("deliveryDays"));
        }
    }
}
