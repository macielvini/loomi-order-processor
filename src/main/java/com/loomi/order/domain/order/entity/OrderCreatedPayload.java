package com.loomi.order.domain.order.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.loomi.order.domain.order.valueobject.OrderItem;
import com.loomi.order.domain.order.valueobject.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedPayload {
    private UUID id;
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
}
