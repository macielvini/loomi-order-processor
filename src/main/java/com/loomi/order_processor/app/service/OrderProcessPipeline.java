package com.loomi.order_processor.app.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.order.dto.OrderProcessResult;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.usecase.OrderHandler;
import com.loomi.order_processor.domain.order.usecase.OrderItemHandler;
import com.loomi.order_processor.domain.order.valueobject.OrderError;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidationResult;
import com.loomi.order_processor.domain.product.exception.ProductNotFoundException;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@Component
public class OrderProcessPipeline {

    private final List<OrderHandler> globalHandlers;
    private final Map<ProductType, OrderItemHandler> byTypeHandlers;
    private final ProductRepository productRepository;

    public OrderProcessPipeline(
            List<OrderHandler> globalHandlers,
            List<OrderItemHandler> handlers,
            ProductRepository productRepository) {
        this.globalHandlers = globalHandlers;
        this.productRepository = productRepository;
        this.byTypeHandlers = handlers.stream()
                .collect(Collectors.toMap(OrderItemHandler::supportedType, Function.identity()));
    }

    private OrderItemHandler getHandlerFor(OrderItem item) {
        return byTypeHandlers.get(item.productType());
    }

    public ValidationResult validate(Order order) {
        for (var item : order.items()) {
            var product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            var productHandler = getHandlerFor(item);

            if (productHandler == null) {
                return ValidationResult.fail(OrderError.INTERNAL_ERROR.toString());
            }

            var validation = productHandler.validate(item, product, order);
            if (!validation.isValid()) {
                return ValidationResult.fail(validation.getErrors());
            }

            if (validation.isHumanReviewRequired()) {
                return ValidationResult.requireHumanReview();
            }
        }

        for (var handler : globalHandlers) {
            var validation = handler.validate(order);
            if (!validation.isValid()) {
                return ValidationResult.fail(validation.getErrors());
            }

            if (validation.isHumanReviewRequired()) {
                return ValidationResult.requireHumanReview();
            }
        }

        return ValidationResult.ok();
    }

    public OrderProcessResult process(Order order) {
        for (var item : order.items()) {
            var product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            var productHandler = byTypeHandlers.get(item.productType());

            if (productHandler == null) {
                return OrderProcessResult.fail(OrderError.INTERNAL_ERROR.toString());
            }

            var processResult = productHandler.process(item, product, order);

            if (!processResult.isProcessed()) {
                return OrderProcessResult.fail(processResult.getErrors());
            }
        }

        for (var handler : globalHandlers) {
            var processResult = handler.process(order);
            if (!processResult.isProcessed()) {
                return OrderProcessResult.fail(processResult.getErrors());
            }
        }
        return OrderProcessResult.ok();
    }
}
