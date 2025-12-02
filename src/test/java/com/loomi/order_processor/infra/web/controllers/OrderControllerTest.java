package com.loomi.order_processor.infra.web.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.usecase.OrderService;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private UUID orderId1;
    private UUID orderId2;
    private String customerId;
    private LocalDateTime testCreatedAt;

    @BeforeEach
    void setUp() {
        orderId1 = UUID.randomUUID();
        orderId2 = UUID.randomUUID();
        customerId = "customer-123";
        testCreatedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    }

    private Order createTestOrder(UUID id, String customerId, BigDecimal totalAmount, OrderStatus status, LocalDateTime createdAt) {
        return Order.builder()
                .id(id)
                .customerId(customerId)
                .totalAmount(totalAmount)
                .status(status)
                .createdAt(createdAt)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should return list of orders when customerId is provided")
    void shouldReturnListOfOrders_whenCustomerIdIsProvided() throws Exception {
        List<Order> orders = List.of(
                createTestOrder(orderId1, customerId, BigDecimal.valueOf(100.50), OrderStatus.PENDING, testCreatedAt),
                createTestOrder(orderId2, customerId, BigDecimal.valueOf(250.75), OrderStatus.PROCESSED, testCreatedAt.plusDays(1))
        );

        when(orderService.findOrdersByCustomerId(customerId)).thenReturn(orders);

        mockMvc.perform(get("/api/orders")
                .param("customerId", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.orders[0].order_id").value(orderId1.toString()))
                .andExpect(jsonPath("$.orders[0].total_amount").value(100.50))
                .andExpect(jsonPath("$.orders[0].status").value("PENDING"))
                .andExpect(jsonPath("$.orders[0].created_at").exists())
                .andExpect(jsonPath("$.orders[1].order_id").value(orderId2.toString()))
                .andExpect(jsonPath("$.orders[1].total_amount").value(250.75))
                .andExpect(jsonPath("$.orders[1].status").value("PROCESSED"))
                .andExpect(jsonPath("$.orders[1].created_at").exists());
    }

    @Test
    @DisplayName("Should return empty list when customer has no orders")
    void shouldReturnEmptyList_whenCustomerHasNoOrders() throws Exception {
        when(orderService.findOrdersByCustomerId(customerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders")
                .param("customerId", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(0));
    }

    @Test
    @DisplayName("Should return empty list when customerId parameter is not provided")
    void shouldReturnEmptyList_whenCustomerIdParameterIsNotProvided() throws Exception {
        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(0));
    }

    @Test
    @DisplayName("Should return empty list when customerId is empty string")
    void shouldReturnEmptyList_whenCustomerIdIsEmptyString() throws Exception {
        mockMvc.perform(get("/api/orders")
                .param("customerId", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(0));
    }

    @Test
    @DisplayName("Should return empty list when customerId is blank")
    void shouldReturnEmptyList_whenCustomerIdIsBlank() throws Exception {
        mockMvc.perform(get("/api/orders")
                .param("customerId", "   ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(0));
    }
}

