package com.loomi.order_processor.domain.product.dto;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;


    public static ValidationResult ok() { 
        return new ValidationResult(true, List.of()); 
    }

    public static ValidationResult fail(String... errors) {
        return new ValidationResult(false, Arrays.asList(errors));
    }
    
    public boolean isValid() { return valid; }
    
    public List<String> getErrors() { return errors; }
}
