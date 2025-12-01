package com.loomi.order_processor.domain.order.dto;

public enum ItemHandlerError {
    OUT_OF_STOCK,
    INTERNAL_ERROR,
    WAREHOUSE_UNAVAILABLE,
    ALREADY_OWNED, 
    LICENSE_UNAVAILABLE, 
    DISTRIBUTION_RIGHTS_EXPIRED
}
