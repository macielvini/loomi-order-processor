package com.loomi.order_processor.infra.web.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.usecase.OrderService;
import com.loomi.order_processor.infra.web.dto.CreateOrderRequest;
import com.loomi.order_processor.infra.web.dto.OrderSummaryResponse;
import com.loomi.order_processor.infra.web.dto.OrdersListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    ResponseEntity<OrdersListResponse> getOrders(@RequestParam(required = false) String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.ok(new OrdersListResponse(List.of()));
        }
        
        var orders = orderService.findOrdersByCustomerId(customerId);
        var orderSummaries = orders.stream()
                .map(order -> new OrderSummaryResponse(
                        order.id(),
                        order.totalAmount(),
                        order.status(),
                        order.createdAt()))
                .toList();
        
        return ResponseEntity.ok(new OrdersListResponse(orderSummaries));
    }

    @GetMapping("/{orderId}")
    ResponseEntity<?> consultOrder(@PathVariable UUID orderId) {
        var order = orderService.consultOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    ResponseEntity<?> createOrder(@RequestBody @Valid CreateOrderRequest body) {
        var createOrder = new CreateOrder(body.customerId(), body.items());
        var order = orderService.createOrder(createOrder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/orders/" + order.id()))
                .body(order);
    }
}
