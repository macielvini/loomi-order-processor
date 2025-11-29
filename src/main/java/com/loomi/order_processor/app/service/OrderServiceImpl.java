package com.loomi.order_processor.app.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.exception.HttpException;
import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.exception.OrderNotFoundException;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.service.OrderService;
import com.loomi.order_processor.domain.product.exception.ProductIsNotActiveException;
import com.loomi.order_processor.domain.product.exception.ProductNotFoundException;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public Order consultOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public Order createOrder(CreateOrder createOrder) {
        var toValidateProducts = productRepository.findAllById(createOrder.items().stream()
                .map(CreateOrderItem::productId)
                .collect(Collectors.toList()));

        if (toValidateProducts.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "No products found");
        }

        var orderItems = new ArrayList<OrderItem>();
        for (var product : toValidateProducts) {
            var currentItem = createOrder.items().stream()
                    .filter(item -> item.productId().equals(product.id()))
                    .findFirst()
                    .orElseThrow(() -> new ProductNotFoundException(product.id()));

            if (!product.isActive()) {
                throw new ProductIsNotActiveException(product.id());
            }

            orderItems.add(OrderItem.fromProduct(product, currentItem.quantity(), currentItem.metadata()));
        }

        var totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        var order = Order.builder()
            .customerId(createOrder.customerId())
            .items(orderItems)
            .status(OrderStatus.PENDING)
            .totalAmount(totalAmount)
            .build();
        var savedOrder = orderRepository.save(order);

        // TODO: public event `OrderCreated` to Kafka
        return savedOrder;
    }
}
