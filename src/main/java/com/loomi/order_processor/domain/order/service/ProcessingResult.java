package com.loomi.order_processor.domain.order.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcessingResult {
    private boolean success;
    private String failureReason;

    public static ProcessingResult success() {
        return new ProcessingResult(true, null);
    }

    public static ProcessingResult failure(String reason) {
        return new ProcessingResult(false, reason);
    }
}

