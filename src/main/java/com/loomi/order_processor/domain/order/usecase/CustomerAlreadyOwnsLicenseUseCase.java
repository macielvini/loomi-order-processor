package com.loomi.order_processor.domain.order.usecase;

import java.util.UUID;

public interface CustomerAlreadyOwnsLicenseUseCase {
    boolean customerAlreadyOwnsLicense(String customerId, UUID productId);
}
