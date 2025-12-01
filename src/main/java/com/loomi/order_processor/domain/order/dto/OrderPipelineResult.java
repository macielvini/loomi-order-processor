package com.loomi.order_processor.domain.order.dto;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

public class OrderPipelineResult {
    private boolean success;
    @Getter
    private List<String> errors;

    public boolean isOk() {
        return success;
    }

    private OrderPipelineResult(boolean success, List<String> errors) {
        this.success = success;
        this.errors = errors;
    }

    public static OrderPipelineResult ok() {
        return new OrderPipelineResult(true, null);
    }

    public static OrderPipelineResult fail(String... errors) {
        return new OrderPipelineResult(false, Arrays.asList(errors));
    }

    public static OrderPipelineResult fail(List<String> errors) {
        return new OrderPipelineResult(false, errors);
    }
}
