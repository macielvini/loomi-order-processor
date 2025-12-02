package com.loomi.order_processor.domain.order.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
@DisplayName("Corporate Product Handler Tests")
class CorporateProductValidationTest {

    private UUID testProductId;
    private UUID testOrderId;
    private String testCustomerId;

    @InjectMocks
    private CorporateItemHandler corporateItemHandler;

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
                .productType(ProductType.CORPORATE)
                .price(price)
                .metadata(metadata)
                .build();
    }

    private Product createProduct(boolean isActive) {
        return Product.builder()
                .id(testProductId)
                .name("Test Corporate Product")
                .productType(ProductType.CORPORATE)
                .price(BigDecimal.valueOf(1000.00))
                .isActive(isActive)
                .metadata(new RawProductMetadata())
                .build();
    }

    private RawProductMetadata createMetadata(String cnpj, String paymentTerms) {
        RawProductMetadata metadata = new RawProductMetadata();
        if (cnpj != null) {
            metadata.put("cnpj", cnpj);
        }
        if (paymentTerms != null) {
            metadata.put("paymentTerms", paymentTerms);
        }
        return metadata;
    }

    private Order createOrder(OrderItem item, BigDecimal totalAmount) {
        return Order.builder()
                .id(testOrderId)
                .customerId(testCustomerId)
                .items(java.util.List.of(item))
                .totalAmount(totalAmount)
                .build();
    }

    private Order createOrderWithMultipleItems(List<OrderItem> items, BigDecimal totalAmount) {
        return Order.builder()
                .id(testOrderId)
                .customerId(testCustomerId)
                .items(items)
                .totalAmount(totalAmount)
                .build();
    }

    @Test
    @DisplayName("shouldSupportOnlyCorporateProductType")
    void shouldSupportOnlyCorporateProductType() {
        assertEquals(ProductType.CORPORATE, corporateItemHandler.supportedType());
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("shouldReturnInvalidCorporateData_whenMetadataIsNull")
        void shouldReturnInvalidCorporateData_whenMetadataIsNull() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), null);
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_CORPORATE_DATA.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidCorporateData_whenCnpjIsMissing")
        void shouldReturnInvalidCorporateData_whenCnpjIsMissing() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata(null, "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_CORPORATE_DATA.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidCorporateData_whenPaymentTermsIsMissing")
        void shouldReturnInvalidCorporateData_whenPaymentTermsIsMissing() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", null));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_CORPORATE_DATA.toString()));
        }

        @Test
        @DisplayName("shouldReturnInvalidCorporateData_whenCnpjIsInvalidFormat")
        void shouldReturnInvalidCorporateData_whenCnpjIsInvalidFormat() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("123", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_CORPORATE_DATA.toString()));
        }

        @Test
        @DisplayName("shouldReturnOk_whenCnpjHasValidFormat")
        void shouldReturnOk_whenCnpjHasValidFormat() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenCnpjHasValidFormatWithoutFormatting")
        void shouldReturnOk_whenCnpjHasValidFormatWithoutFormatting() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12345678000190", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnInvalidCorporateData_whenPaymentTermsIsInvalid")
        void shouldReturnInvalidCorporateData_whenPaymentTermsIsInvalid() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_45"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_CORPORATE_DATA.toString()));
        }

        @Test
        @DisplayName("shouldReturnOk_whenPaymentTermsIsNET_30")
        void shouldReturnOk_whenPaymentTermsIsNET_30() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenPaymentTermsIsNET_60")
        void shouldReturnOk_whenPaymentTermsIsNET_60() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_60"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenPaymentTermsIsNET_90")
        void shouldReturnOk_whenPaymentTermsIsNET_90() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_90"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnOk_whenPaymentTermsIsCaseInsensitive")
        void shouldReturnOk_whenPaymentTermsIsCaseInsensitive() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "net_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("shouldReturnInvalidCorporateData_whenProductIsInactive")
        void shouldReturnInvalidCorporateData_whenProductIsInactive() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(false);
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.INVALID_CORPORATE_DATA.toString()));
        }

        @Test
        @DisplayName("shouldReturnCreditLimitExceeded_whenOrderTotalExceedsMaxCreditLimit")
        void shouldReturnCreditLimitExceeded_whenOrderTotalExceedsMaxCreditLimit() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, new BigDecimal("100001"));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains(OrderError.CREDIT_LIMIT_EXCEEDED.toString()));
        }

        @Test
        @DisplayName("shouldRequireHumanReview_whenOrderTotalEqualsMaxCreditLimit")
        void shouldRequireHumanReview_whenOrderTotalEqualsMaxCreditLimit() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, new BigDecimal("100000"));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
            assertTrue(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldRequireHumanReview_whenOrderTotalExceedsHighValueThreshold")
        void shouldRequireHumanReview_whenOrderTotalExceedsHighValueThreshold() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, new BigDecimal("50001"));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
            assertTrue(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldReturnOk_whenOrderTotalEqualsHighValueThreshold")
        void shouldReturnOk_whenOrderTotalEqualsHighValueThreshold() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, new BigDecimal("50000"));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldReturnOk_whenOrderTotalIsBelowHighValueThreshold")
        void shouldReturnOk_whenOrderTotalIsBelowHighValueThreshold() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, new BigDecimal("49999"));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }

        @Test
        @DisplayName("shouldReturnOk_whenAllValidationsPass")
        void shouldReturnOk_whenAllValidationsPass() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Product product = createProduct(true);
            Order order = createOrder(item, new BigDecimal("10000"));

            ValidationResult result = corporateItemHandler.validate(item, product, order);

            assertTrue(result.isValid());
            assertFalse(result.isHumanReviewRequired());
        }
    }

    @Nested
    @DisplayName("Process Tests")
    class ProcessTests {

        @Test
        @DisplayName("shouldApplyVolumeDiscount_whenQuantityExceedsThresholdWithSingleBlock")
        void shouldApplyVolumeDiscount_whenQuantityExceedsThresholdWithSingleBlock() {
            BigDecimal price = BigDecimal.valueOf(10.00);
            OrderItem item = createOrderItem(150, price, createMetadata("12.345.678/0001-90", "NET_30"));
            Order order = createOrder(item, BigDecimal.valueOf(1500.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertTrue(item.metadata().containsKey("discountAmount"));
            BigDecimal discountAmount = (BigDecimal) item.metadata().get("discountAmount");
            BigDecimal expectedDiscount = price
                    .multiply(BigDecimal.valueOf(100))
                    .multiply(BigDecimal.valueOf(0.15));
            assertEquals(expectedDiscount, discountAmount);
        }

        @Test
        @DisplayName("shouldNotApplyVolumeDiscount_whenQuantityIsBelowThreshold")
        void shouldNotApplyVolumeDiscount_whenQuantityIsBelowThreshold() {
            BigDecimal price = BigDecimal.valueOf(10.00);
            OrderItem item = createOrderItem(50, price, createMetadata("12.345.678/0001-90", "NET_30"));
            Order order = createOrder(item, BigDecimal.valueOf(500.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertFalse(item.metadata().containsKey("discountAmount"));
        }

        @Test
        @DisplayName("shouldApplyVolumeDiscount_whenQuantityEqualsThreshold")
        void shouldApplyVolumeDiscount_whenQuantityEqualsThreshold() {
            BigDecimal price = BigDecimal.valueOf(10.00);
            OrderItem item = createOrderItem(100, price, createMetadata("12.345.678/0001-90", "NET_30"));
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertTrue(item.metadata().containsKey("discountAmount"));
            BigDecimal discountAmount = (BigDecimal) item.metadata().get("discountAmount");
            BigDecimal expectedDiscount = price
                    .multiply(BigDecimal.valueOf(100))
                    .multiply(BigDecimal.valueOf(0.15));
            assertEquals(expectedDiscount, discountAmount);
        }

        @Test
        @DisplayName("shouldCalculateDiscountCorrectly_whenQuantityHasTwoBlocks")
        void shouldCalculateDiscountCorrectly_whenQuantityHasTwoBlocks() {
            BigDecimal price = BigDecimal.valueOf(10.00);
            OrderItem item = createOrderItem(250, price, createMetadata("12.345.678/0001-90", "NET_30"));
            Order order = createOrder(item, BigDecimal.valueOf(2500.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            BigDecimal discountAmount = (BigDecimal) item.metadata().get("discountAmount");
            BigDecimal expectedDiscount = price
                    .multiply(BigDecimal.valueOf(200))
                    .multiply(BigDecimal.valueOf(0.15));
            assertEquals(expectedDiscount, discountAmount);
        }

        @Test
        @DisplayName("shouldApplyDiscountToAllCorporateItems_whenThresholdExceeded")
        void shouldApplyDiscountToAllCorporateItems_whenThresholdExceeded() {
            BigDecimal price = BigDecimal.valueOf(10.00);
            OrderItem item1 = createOrderItem(150, price, createMetadata("12.345.678/0001-90", "NET_30"));
            OrderItem item2 = createOrderItem(250, price, createMetadata("12.345.678/0001-90", "NET_30"));
            List<OrderItem> items = List.of(item1, item2);
            Order order = createOrderWithMultipleItems(items, BigDecimal.valueOf(4000.00));
            Product product = createProduct(true);

            corporateItemHandler.process(item1, product, order);
            corporateItemHandler.process(item2, product, order);

            assertTrue(item1.metadata().containsKey("discountAmount"));
            assertTrue(item2.metadata().containsKey("discountAmount"));
            BigDecimal discount1 = (BigDecimal) item1.metadata().get("discountAmount");
            BigDecimal discount2 = (BigDecimal) item2.metadata().get("discountAmount");
            BigDecimal expectedDiscount1 = price
                    .multiply(BigDecimal.valueOf(100))
                    .multiply(BigDecimal.valueOf(0.15));
            BigDecimal expectedDiscount2 = price
                    .multiply(BigDecimal.valueOf(200))
                    .multiply(BigDecimal.valueOf(0.15));
            assertEquals(expectedDiscount1, discount1);
            assertEquals(expectedDiscount2, discount2);
        }

        @Test
        @DisplayName("shouldConfigurePaymentTermsInMetadata")
        void shouldConfigurePaymentTermsInMetadata() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_60"));
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertTrue(item.metadata().containsKey("paymentTerms"));
            assertEquals("NET_60", item.metadata().get("paymentTerms"));
        }

        @Test
        @DisplayName("shouldProcessSuccessfully_whenAllConditionsMet")
        void shouldProcessSuccessfully_whenAllConditionsMet() {
            OrderItem item = createOrderItem(1, BigDecimal.valueOf(1000.00), createMetadata("12.345.678/0001-90", "NET_30"));
            Order order = createOrder(item, BigDecimal.valueOf(1000.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(item, product, order);

            assertTrue(result.isProcessed());
            assertTrue(item.metadata().containsKey("paymentTerms"));
        }

        @Test
        @DisplayName("shouldOnlyCountCorporateItemsForVolumeDiscount")
        void shouldOnlyCountCorporateItemsForVolumeDiscount() {
            BigDecimal price = BigDecimal.valueOf(10.00);
            OrderItem corporateItem = createOrderItem(250, price, createMetadata("12.345.678/0001-90", "NET_30"));
            OrderItem physicalItem = OrderItem.builder()
                    .productId(UUID.randomUUID())
                    .quantity(500)
                    .productType(ProductType.PHYSICAL)
                    .price(price)
                    .metadata(new RawProductMetadata())
                    .build();
            List<OrderItem> items = List.of(corporateItem, physicalItem);
            Order order = createOrderWithMultipleItems(items, BigDecimal.valueOf(7500.00));
            Product product = createProduct(true);

            OrderProcessResult result = corporateItemHandler.process(corporateItem, product, order);

            assertTrue(result.isProcessed());
            assertTrue(corporateItem.metadata().containsKey("discountAmount"));
            BigDecimal discountAmount = (BigDecimal) corporateItem.metadata().get("discountAmount");
            BigDecimal expectedDiscount = price
                    .multiply(BigDecimal.valueOf(200))
                    .multiply(BigDecimal.valueOf(0.15));
            assertEquals(expectedDiscount, discountAmount);
        }
    }
}

