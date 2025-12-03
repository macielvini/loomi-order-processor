package com.loomi.order.domain.order.dto;

import java.util.List;


public record CreateOrder(
    String customerId,
    List<CreateOrderItem> items
) {
    
}
