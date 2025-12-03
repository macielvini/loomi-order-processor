package com.loomi.order.app.service.order.handler;

import com.loomi.order.domain.order.dto.OrderProcessResult;
import com.loomi.order.domain.order.entity.Order;
import com.loomi.order.domain.order.valueobject.OrderItem;
import com.loomi.order.domain.product.dto.ProductType;
import com.loomi.order.domain.product.dto.ValidationResult;
import com.loomi.order.domain.product.entity.Product;

public interface OrderItemHandler {
    public ProductType supportedType();
    
    public ValidationResult validate(OrderItem item, Product product, Order ctx);

    public OrderProcessResult process(OrderItem item, Product product, Order ctx);
}
