package com.loomi.order_processor.app.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.app.utils.JsonUtils;
import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.dto.CreateOrderItem;
import com.loomi.order_processor.domain.order.dto.OrderItem;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.exception.OrderNotFoundException;
import com.loomi.order_processor.domain.order.repository.OrderRepository;
import com.loomi.order_processor.domain.order.service.OrderService;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.dto.ValidatorMap;
import com.loomi.order_processor.domain.product.exception.ProductValidationException;
import com.loomi.order_processor.domain.product.exception.ProductNotFoundException;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ValidatorMap validatorMap;

    @Override
    public Order consultOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public UUID createOrder(CreateOrder createOrder) {
        var validationErrors = new ArrayList<ValidationResult>();

        for (var item : createOrder.items()) {
            var results = validateOrderItem(item);
            if (!results.isEmpty()) {
                validationErrors.addAll(results);
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new ProductValidationException(JsonUtils.toJson(validationErrors));
        }

        var orderItems = getOrderItemsSnapshot(createOrder.items());

        var totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var order = Order.builder()
                .customerId(createOrder.customerId())
                .items(orderItems)
                .totalAmount(totalAmount)
                .build();

        var savedOrder = orderRepository.save(order);
        return savedOrder.id();
    }

    private List<OrderItem> getOrderItemsSnapshot(List<CreateOrderItem> items) {
        var uniqueIds = items.stream()
                .map(CreateOrderItem::productId)
                .distinct()
                .collect(Collectors.toList());
        var products = productRepository.findAllById(uniqueIds);

        var productMap = products.stream()
                .collect(Collectors.toMap(
                        p -> p.id(),
                        p -> p));

        return items.stream()
                .map(item -> {
                    var product = productMap.get(item.productId());
                    return OrderItem.fromProduct(product, item.quantity(), item.metadata());
                })
                .collect(Collectors.toList());
    }

    private List<ValidationResult> validateOrderItem(CreateOrderItem item) {
        var product = productRepository
                .findById(item.productId())
                .orElseThrow(() -> new ProductNotFoundException(item.productId()));
        var productValidators = validatorMap.getValidatorsFor(product);

        List<ValidationResult> results = new ArrayList<>();

        for (var validator : productValidators) {
            var result = validator.validate(product);
            if (!result.isValid()) {
                results.add(result);
            }
        }

        return results;
    }

}
