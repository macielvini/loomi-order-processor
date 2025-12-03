package com.loomi.order_processor.domain.order.usecase;

import java.math.BigDecimal;

public interface IsManualValidationRequiredUseCase {
    boolean checkValueRequiresManualValidation(BigDecimal value);
}
