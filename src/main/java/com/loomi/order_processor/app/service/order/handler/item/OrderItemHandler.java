package com.loomi.order_processor.app.service.order.handler.item;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.entity.Product;

public interface OrderItemHandler {
    public ProductType supportedType();
    
    public ValidationResult validate(OrderItem item, Product product, Order ctx);

    public OrderProcessResult process(OrderItem item, Product product, Order ctx);
}
