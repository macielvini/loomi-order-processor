package com.loomi.order_processor.domain.order.usecase;

import java.math.BigDecimal;
import java.util.Set;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class CorporateItemHandler implements OrderItemHandler {

    private static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("100000");
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000");
    private static final int VOLUME_DISCOUNT_THRESHOLD = 100;
    private static final double VOLUME_DISCOUNT_PERCENTAGE = 0.15;
    private static final Set<String> VALID_PAYMENT_TERMS = Set.of("NET_30", "NET_60", "NET_90");
    private static final int CNPJ_LENGTH = 14;

    private ValidationResult validateMetadata(OrderItem item) {
        if (item.metadata() == null) {
            log.warn("Missing metadata for corporate product {}", item.productId());
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        var cnpj = item.metadata().getOrDefault("cnpj", "").toString();
        if (StringUtils.isBlank(cnpj)) {
            log.warn("Missing cnpj in metadata for product {}", item.productId());
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        var paymentTerms = item.metadata().getOrDefault("paymentTerms", "").toString();
        if (StringUtils.isBlank(paymentTerms)) {
            log.warn("Missing paymentTerms in metadata for product {}", item.productId());
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        return ValidationResult.ok();
    }

    private String normalizeCnpj(String cnpj) {
        return cnpj.replaceAll("[^0-9]", "");
    }

    private boolean isValidCnpjFormat(String cnpj) {
        String normalized = normalizeCnpj(cnpj);
        return normalized.length() == CNPJ_LENGTH;
    }

    private String getCnpj(OrderItem item) {
        var cnpj = item.metadata().get("cnpj");
        return cnpj != null ? cnpj.toString() : "";
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
        var metadataValidation = validateMetadata(item);
        if (!metadataValidation.isValid()) {
            return metadataValidation;
        }

        String cnpj = getCnpj(item);
        if (!isValidCnpjFormat(cnpj)) {
            log.error("Invalid CNPJ format for product {}: {}", item.productId(), cnpj);
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        String paymentTerms = getPaymentTerms(item);
        if (!VALID_PAYMENT_TERMS.contains(paymentTerms)) {
            log.error("Invalid paymentTerms for product {} in order {}: {}", item.productId(), ctx.id(), paymentTerms);
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        if (!product.isActive()) {
            log.error("Product {} is not active", item.productId());
            return ValidationResult.fail(OrderError.INVALID_CORPORATE_DATA.toString());
        }

        BigDecimal orderTotal = ctx.totalAmount();
        if (orderTotal.compareTo(MAX_CREDIT_LIMIT) > 0) {
            log.info("Order {} with total ${} exceeds credit limit of ${} for customer {}",
                    ctx.id(), orderTotal, MAX_CREDIT_LIMIT, ctx.customerId());
            return ValidationResult.fail(OrderError.CREDIT_LIMIT_EXCEEDED.toString());
        }

        if (orderTotal.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            log.info("Order {} requires manual approval: total amount ${} exceeds threshold of ${}",
                    ctx.id(), orderTotal, HIGH_VALUE_THRESHOLD);
            return ValidationResult.requireHumanReview();
        }

        return ValidationResult.ok();
    }

    @Override
    public OrderProcessResult process(OrderItem item, Product product, Order ctx) {
        if (item.quantity() >= VOLUME_DISCOUNT_THRESHOLD) {
            int blocks = item.quantity() / VOLUME_DISCOUNT_THRESHOLD;
            BigDecimal unitPrice = item.price();

            BigDecimal discountUnits = BigDecimal.valueOf(blocks * VOLUME_DISCOUNT_THRESHOLD);
            BigDecimal discountBase = unitPrice.multiply(discountUnits);
            BigDecimal discountAmount = discountBase.multiply(BigDecimal.valueOf(VOLUME_DISCOUNT_PERCENTAGE));


            item.metadata().put("discountAmount", discountAmount);
            log.info("Applied volume discount of {}% to corporate item {} in order {}: quantity={}, blocks={}, unitPrice={}, discountAmount={}",
                    VOLUME_DISCOUNT_PERCENTAGE * 100, item.productId(), ctx.id(), item.quantity(), blocks, unitPrice, discountAmount);
        }

        String paymentTerms = getPaymentTerms(item);
        item.metadata().put("paymentTerms", paymentTerms);
        log.info("Configured payment terms {} for corporate item {}", paymentTerms, item.productId());

        return OrderProcessResult.ok();
    }
}
