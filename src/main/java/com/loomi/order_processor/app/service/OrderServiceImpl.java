package com.loomi.order_processor.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

import com.loomi.order_processor.app.utils.JsonUtils;
import com.loomi.order_processor.domain.order.dto.CreateOrder;
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

        var order = Order.builder()
                .customerId(createOrder.customerId())
                .items(createOrder.items())
                .build();

        var savedOrder = orderRepository.save(order);
        return savedOrder.id();
    }

    private List<ValidationResult> validateOrderItem(OrderItem item) {
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
