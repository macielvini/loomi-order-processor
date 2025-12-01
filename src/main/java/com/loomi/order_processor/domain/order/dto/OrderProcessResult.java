package com.loomi.order_processor.domain.order.dto;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

public class OrderProcessResult {
    private boolean isProcessed;
    @Getter
    private List<String> errors;

    private OrderProcessResult(boolean isProcessed, List<String> errors) {
        this.isProcessed = isProcessed;
        this.errors = errors;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public boolean isFailed() {
        return errors != null && !errors.isEmpty();
    }

    public static OrderProcessResult ok() {
        return new OrderProcessResult(true, null);
    }

    public static OrderProcessResult fail(String... errors) {
        return new OrderProcessResult(false, Arrays.asList(errors));
    }

    public static OrderProcessResult fail(List<String> errors) {
        return new OrderProcessResult(false, errors);
    }
}
