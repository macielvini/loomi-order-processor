package com.loomi.order_processor.domain.order.dto;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

public class OrderProcessResult {
    private boolean isProcessed;
    @Getter
    private List<String> errors;
    @Getter
    private boolean isHumanReviewRequired;

    private OrderProcessResult(boolean isProcessed, List<String> errors, boolean requiresAnalysis) {
        this.isProcessed = isProcessed;
        this.errors = errors;
        this.isHumanReviewRequired = requiresAnalysis;
    }

    public boolean isProcessed() {
        return isProcessed && !isHumanReviewRequired;
    }

    public boolean isFailed() {
        return errors != null && !errors.isEmpty();
    }

    public static OrderProcessResult ok() {
        return new OrderProcessResult(true, null, false);
    }

    public static OrderProcessResult fail(String... errors) {
        return new OrderProcessResult(false, Arrays.asList(errors), false);
    }

    public static OrderProcessResult fail(List<String> errors) {
        return new OrderProcessResult(false, errors, false);
    }

    public static OrderProcessResult requireHumanReview() {
        return new OrderProcessResult(false, null, true);
    }
}
