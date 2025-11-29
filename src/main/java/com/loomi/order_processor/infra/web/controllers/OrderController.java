package com.loomi.order_processor.infra.web.controllers;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loomi.order_processor.domain.order.dto.CreateOrder;
import com.loomi.order_processor.domain.order.service.OrderService;
import com.loomi.order_processor.infra.web.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    ResponseEntity<?> consultOrder(@PathVariable UUID orderId) {
        var order = orderService.consultOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    ResponseEntity<?> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        var createOrder = new CreateOrder(request.customerId(), request.items());
        var order = orderService.createOrder(createOrder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/orders/" + order.id()))
                .body(order);
    }
}
