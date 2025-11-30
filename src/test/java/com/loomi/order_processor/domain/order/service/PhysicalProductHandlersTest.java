package com.loomi.order_processor.domain.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
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

import com.loomi.order_processor.domain.order.dto.ItemHandlerError;
import com.loomi.order_processor.domain.order.dto.ItemHandlerResult;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.entity.LowStockAlertEvent;
import com.loomi.order_processor.domain.order.producer.AlertProducer;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Physical Product Handlers Tests")
class PhysicalProductHandlersTest {

    private UUID testProductId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
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

    @Nested
    @DisplayName("StockHandler Tests")
    class StockHandlerTests {

        @Mock
        private ProductRepository productRepository;

        @Mock
        private AlertProducer alertProducer;

        @InjectMocks
        private StockHandler stockHandler;

        @Test
        @DisplayName("shouldReturnInternalError_whenProductNotFound")
        void shouldReturnInternalError_whenProductNotFound() {
            OrderItem item = createOrderItem(2, new RawProductMetadata());
            when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

            ItemHandlerResult result = stockHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.INTERNAL_ERROR, result.getError());
            verify(productRepository).findById(testProductId);
            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldReturnOutOfStock_whenProductIsInactive")
        void shouldReturnOutOfStock_whenProductIsInactive() {
            OrderItem item = createOrderItem(2, new RawProductMetadata());
            Product product = createProduct(100, false);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.OUT_OF_STOCK, result.getError());
            verify(productRepository).findById(testProductId);
            verify(productRepository, never()).update(any());
            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldReturnOutOfStock_whenStockQuantityIsNull")
        void shouldReturnOutOfStock_whenStockQuantityIsNull() {
            OrderItem item = createOrderItem(2, new RawProductMetadata());
            Product product = createProduct(null, true);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.OUT_OF_STOCK, result.getError());
            verify(productRepository).findById(testProductId);
            verify(productRepository, never()).update(any());
            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldReturnOutOfStock_whenStockQuantityIsLessThanRequired")
        void shouldReturnOutOfStock_whenStockQuantityIsLessThanRequired() {
            OrderItem item = createOrderItem(10, new RawProductMetadata());
            Product product = createProduct(5, true);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.OUT_OF_STOCK, result.getError());
            verify(productRepository).findById(testProductId);
            verify(productRepository, never()).update(any());
            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldReserveStockSuccessfully_whenStockIsSufficient")
        void shouldReserveStockSuccessfully_whenStockIsSufficient() {
            OrderItem item = createOrderItem(5, new RawProductMetadata());
            Product product = createProduct(100, true);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertTrue(result.isValid());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(95, updatedProduct.stockQuantity());
            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldSendLowStockAlert_whenRemainingStockIsBelowThreshold")
        void shouldSendLowStockAlert_whenRemainingStockIsBelowThreshold() {
            OrderItem item = createOrderItem(10, new RawProductMetadata());
            Product product = createProduct(12, true);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertTrue(result.isValid());
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
            OrderItem item = createOrderItem(5, new RawProductMetadata());
            Product product = createProduct(10, true);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertTrue(result.isValid());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(5, updatedProduct.stockQuantity());

            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldNotSendLowStockAlert_whenRemainingStockIsAboveThreshold")
        void shouldNotSendLowStockAlert_whenRemainingStockIsAboveThreshold() {
            OrderItem item = createOrderItem(5, new RawProductMetadata());
            Product product = createProduct(20, true);
            when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

            ItemHandlerResult result = stockHandler.handle(item);

            assertTrue(result.isValid());
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updatedProduct = productCaptor.getValue();
            assertEquals(15, updatedProduct.stockQuantity());

            verify(alertProducer, never()).sendLowStockAlert(any());
        }

        @Test
        @DisplayName("shouldSupportOnlyPhysicalProductType")
        void shouldSupportOnlyPhysicalProductType() {
            assertTrue(stockHandler.supports(ProductType.PHYSICAL));
            assertFalse(stockHandler.supports(ProductType.SUBSCRIPTION));
            assertFalse(stockHandler.supports(ProductType.DIGITAL));
            assertFalse(stockHandler.supports(ProductType.PRE_ORDER));
            assertFalse(stockHandler.supports(ProductType.CORPORATE));
        }
    }

    @Nested
    @DisplayName("DeliveryTimeHandler Tests")
    class DeliveryTimeHandlerTests {

        @InjectMocks
        private DeliveryTimeHandler deliveryTimeHandler;

        @Test
        @DisplayName("shouldUseDefaultDeliveryTime_whenMetadataIsNull")
        void shouldUseDefaultDeliveryTime_whenMetadataIsNull() {
            OrderItem item = createOrderItem(1, null);

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldUseDefaultDeliveryTime_whenWarehouseLocationIsMissing")
        void shouldUseDefaultDeliveryTime_whenWarehouseLocationIsMissing() {
            OrderItem item = createOrderItem(1, new RawProductMetadata());

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldCalculate5Days_whenLocationIsSP")
        void shouldCalculate5Days_whenLocationIsSP() {
            OrderItem item = createOrderItem(1, createMetadata("SP"));

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldCalculate7Days_whenLocationIsRJ")
        void shouldCalculate7Days_whenLocationIsRJ() {
            OrderItem item = createOrderItem(1, createMetadata("RJ"));

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldUseDefaultDeliveryTime_whenLocationIsUnknown")
        void shouldUseDefaultDeliveryTime_whenLocationIsUnknown() {
            OrderItem item = createOrderItem(1, createMetadata("MG"));

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldHandleCaseInsensitivity_whenLocationIsLowerCase")
        void shouldHandleCaseInsensitivity_whenLocationIsLowerCase() {
            OrderItem item = createOrderItem(1, createMetadata("sp"));

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldHandleCaseInsensitivity_whenLocationIsMixedCase")
        void shouldHandleCaseInsensitivity_whenLocationIsMixedCase() {
            OrderItem item = createOrderItem(1, createMetadata("Sp"));

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldHandleWhitespace_whenLocationHasSpaces")
        void shouldHandleWhitespace_whenLocationHasSpaces() {
            OrderItem item = createOrderItem(1, createMetadata("  SP  "));

            ItemHandlerResult result = deliveryTimeHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldSupportOnlyPhysicalProductType")
        void shouldSupportOnlyPhysicalProductType() {
            assertTrue(deliveryTimeHandler.supports(ProductType.PHYSICAL));
            assertFalse(deliveryTimeHandler.supports(ProductType.SUBSCRIPTION));
            assertFalse(deliveryTimeHandler.supports(ProductType.DIGITAL));
            assertFalse(deliveryTimeHandler.supports(ProductType.PRE_ORDER));
            assertFalse(deliveryTimeHandler.supports(ProductType.CORPORATE));
        }
    }

    @Nested
    @DisplayName("WarehouseValidationHandler Tests")
    class WarehouseValidationHandlerTests {

        @InjectMocks
        private WarehouseValidationHandler warehouseValidationHandler;

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenMetadataIsNull")
        void shouldReturnWarehouseUnavailable_whenMetadataIsNull() {
            OrderItem item = createOrderItem(1, null);

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.WAREHOUSE_UNAVAILABLE, result.getError());
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenWarehouseLocationIsMissing")
        void shouldReturnWarehouseUnavailable_whenWarehouseLocationIsMissing() {
            OrderItem item = createOrderItem(1, new RawProductMetadata());

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.WAREHOUSE_UNAVAILABLE, result.getError());
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenWarehouseLocationIsEmpty")
        void shouldReturnWarehouseUnavailable_whenWarehouseLocationIsEmpty() {
            OrderItem item = createOrderItem(1, createMetadata(""));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.WAREHOUSE_UNAVAILABLE, result.getError());
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenWarehouseLocationIsWhitespace")
        void shouldReturnWarehouseUnavailable_whenWarehouseLocationIsWhitespace() {
            OrderItem item = createOrderItem(1, createMetadata("   "));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.WAREHOUSE_UNAVAILABLE, result.getError());
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenLocationIsInvalid")
        void shouldReturnWarehouseUnavailable_whenLocationIsInvalid() {
            OrderItem item = createOrderItem(1, createMetadata("INVALID"));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.WAREHOUSE_UNAVAILABLE, result.getError());
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsSP")
        void shouldReturnOk_whenLocationIsSP() {
            OrderItem item = createOrderItem(1, createMetadata("SP"));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsRJ")
        void shouldReturnOk_whenLocationIsRJ() {
            OrderItem item = createOrderItem(1, createMetadata("RJ"));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenLocationIsMG")
        void shouldReturnOk_whenLocationIsMG() {
            OrderItem item = createOrderItem(1, createMetadata("MG"));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldHandleWhitespace_whenLocationHasSpaces")
        void shouldHandleWhitespace_whenLocationHasSpaces() {
            OrderItem item = createOrderItem(1, createMetadata("  SP  "));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnWarehouseUnavailable_whenLocationIsCaseSensitive")
        void shouldReturnWarehouseUnavailable_whenLocationIsCaseSensitive() {
            OrderItem item = createOrderItem(1, createMetadata("sp"));

            ItemHandlerResult result = warehouseValidationHandler.handle(item);

            assertFalse(result.isValid());
            assertEquals(ItemHandlerError.WAREHOUSE_UNAVAILABLE, result.getError());
        }

        @Test
        @DisplayName("shouldSupportOnlyPhysicalProductType")
        void shouldSupportOnlyPhysicalProductType() {
            assertTrue(warehouseValidationHandler.supports(ProductType.PHYSICAL));
            assertFalse(warehouseValidationHandler.supports(ProductType.SUBSCRIPTION));
            assertFalse(warehouseValidationHandler.supports(ProductType.DIGITAL));
            assertFalse(warehouseValidationHandler.supports(ProductType.PRE_ORDER));
            assertFalse(warehouseValidationHandler.supports(ProductType.CORPORATE));
        }
    }
}

