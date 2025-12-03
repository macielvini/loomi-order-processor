package com.loomi.order.domain.product.dto;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidationResult {
    private final List<String> errors;
    private boolean isHumanReviewRequired;


    public static ValidationResult ok() { 
        return new ValidationResult(List.of(), false); 
    }

    public static ValidationResult fail(String... errors) {
        return new ValidationResult(Arrays.asList(errors), false);
    }

    public static ValidationResult fail(List<String> errors) {
        return new ValidationResult(errors, false);
    }

    public static ValidationResult requireHumanReview() {
        return new ValidationResult(null, true);
    }

    public boolean isValid() { return errors == null || errors.isEmpty(); }

    public boolean isHumanReviewRequired() { return isHumanReviewRequired; }
    
    public List<String> getErrors() { return errors; }
}
