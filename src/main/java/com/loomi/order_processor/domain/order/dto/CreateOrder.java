package com.loomi.order_processor.domain.order.dto;

import java.util.List;


public record CreateOrder(
    String customerId,
    List<CreateOrderItem> items
) {
    
}
