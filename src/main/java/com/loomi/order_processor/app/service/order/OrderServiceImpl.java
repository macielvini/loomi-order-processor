package com.loomi.order_processor.app.service.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.exception.HttpException;
import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.exception.OrderNotFoundException;
import com.loomi.order_processor.domain.event.usecase.OrderEventPublisher;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.usecase.OrderService;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.exception.ProductIsNotActiveException;
import com.loomi.order_processor.domain.product.exception.ProductNotFoundException;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Override
    public Order consultOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public List<Order> findOrdersByCustomerId(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrder createOrder) {
        var order = buildOrderWithPriceSnapshot(createOrder);
        var savedOrder = orderRepository.save(order);

        orderEventPublisher.sendOrderCreatedEvent(OrderCreatedEvent.fromOrder(savedOrder));
        return savedOrder;
    }

    private Order buildOrderWithPriceSnapshot(CreateOrder createOrder) {
        var toValidateProducts = productRepository.findAllById(createOrder.items().stream()
                .map(CreateOrderItem::productId)
                .collect(Collectors.toList()));

        if (toValidateProducts.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "No products found");
        }

        var orderItems = new ArrayList<OrderItem>();
        for (var item : createOrder.items()) {
            var product = toValidateProducts.stream()
                    .filter(p -> p.id().equals(item.productId()))
                    .findFirst()
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            
            if (!product.isActive()) {
                throw new ProductIsNotActiveException(product.id());
            }

            var mergedMetadata = new RawProductMetadata();
            mergedMetadata.putAll(product.metadata());
            mergedMetadata.putAll(item.metadata());

            orderItems.add(OrderItem.fromProduct(
                    product,
                    createOrder.customerId(),
                    item.quantity(),
                    mergedMetadata));
        }

        var totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Order.builder()
                .customerId(createOrder.customerId())
                .items(orderItems)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();
                
    }
}
