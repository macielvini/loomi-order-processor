package com.loomi.order_processor.app.service.order.handler.item;

import java.math.BigDecimal;
import java.util.Set;

import com.loomi.order_processor.domain.order.usecase.*;
import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorporateItemHandler implements
        OrderItemHandler,
        IsManualValidationRequiredUseCase,
        IsItemAvailableForPurchaseUseCase,
        CorporateOrderValidationUseCase,
        ValidateCorporateInformationUseCase {

    private static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("100000");
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000");
    private static final int VOLUME_DISCOUNT_THRESHOLD = 100;
    private static final double VOLUME_DISCOUNT_PERCENTAGE = 0.15;
    private static final Set<String> VALID_PAYMENT_TERMS = Set.of("NET_30", "NET_60", "NET_90");

    private String normalizeString(String str) {
        return str.replaceAll("[^0-9]", "");
    }

    private String getPaymentTerms(OrderItem item) {
        var paymentTerms = item.metadata().getOrDefault("paymentTerms", "NET_30").toString();
        return paymentTerms.trim().toUpperCase();
    }

    @Override
    public ProductType supportedType() {
        return ProductType.CORPORATE;
    }

    @Override
    public ValidationResult validate(OrderItem item, Product product, Order ctx) {
        if (!this.orderHasRequiredCustomerInformation(item)) {
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        if (!this.isItemAvailable(item, product)) {
            return ValidationResult.fail(OrderError.OUT_OF_STOCK.toString());
        }

        if (!this.hasEnoughCreditForOrder(ctx)) {
            return ValidationResult.fail(OrderError.CREDIT_LIMIT_EXCEEDED.toString());
        }

        if (this.checkValueRequiresManualValidation(ctx.totalAmount())) {
            return ValidationResult.requireHumanReview();
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        if (this.shouldApplyDiscount(item)) {
            int blocks = item.quantity() / VOLUME_DISCOUNT_THRESHOLD;
            BigDecimal unitPrice = item.price();

            BigDecimal discountUnits = BigDecimal.valueOf(blocks * VOLUME_DISCOUNT_THRESHOLD);
            BigDecimal discountBase = unitPrice.multiply(discountUnits);
            BigDecimal discountAmount = discountBase.multiply(BigDecimal.valueOf(VOLUME_DISCOUNT_PERCENTAGE));
            item.metadata().put("discountAmount", discountAmount);
        }

        String paymentTerms = getPaymentTerms(item);
        item.metadata().put("paymentTerms", paymentTerms);
        log.info("Configured payment terms {} for corporate item {}", paymentTerms, item.productId());

        return OrderProcessResult.ok();
    }

    @Override
    public boolean checkValueRequiresManualValidation(BigDecimal value) {
        return value.compareTo(HIGH_VALUE_THRESHOLD) > 0;
    }

    @Override
    public boolean isCnpjValid(String cnpj) {
        String normalized = normalizeString(cnpj);
        return normalized.length() == 14;
    }

    @Override
    public boolean isInscricaoEstadualValid(String ie) {
        String normalized = normalizeString(ie);
        return normalized.length() >= 9 && normalized.length() <= 13;
    }

    @Override
    public boolean orderHasRequiredCustomerInformation(OrderItem item) {
        if (isNull(item.metadata())) {
            return false;
        }

        var cnpj = item.metadata().getOrDefault("cnpj", "").toString();
        var ie = item.metadata().getOrDefault("ie", "").toString();
        if (StringUtils.isBlank(cnpj) || StringUtils.isBlank(ie)) {
            return false;
        }

        var paymentTerms = item.metadata().getOrDefault("paymentTerms", "").toString();
        if (StringUtils.isBlank(paymentTerms) || !VALID_PAYMENT_TERMS.contains(paymentTerms.toUpperCase())) {
            log.warn("Missing paymentTerms in metadata for product {}", item.productId());
            return false;
        }

        if (!this.isCnpjValid(cnpj) || !this.isInscricaoEstadualValid(ie)) {
            log.error("Invalid customer information on Order  {}: {}", item.productId(), item.metadata());
            return false;
        }

        return true;
    }

    @Override
    public boolean shouldApplyDiscount(OrderItem item) {
        return item.quantity() >= VOLUME_DISCOUNT_THRESHOLD;
    }

    @Override
    public boolean hasEnoughCreditForOrder(Order order) {
        return order.totalAmount().compareTo(MAX_CREDIT_LIMIT) <= 0;
    }

    @Override
    public boolean isItemAvailable(OrderItem item, Product ctx) {
        return ctx.isActive() && nonNull(ctx.stockQuantity()) && ctx.stockQuantity() >= item.quantity();
    }
}
