package com.loomi.order_processor.infra.persistence.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.loomi.order_processor.domain.order.dto.OrderStatus;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.order.repository.OrderRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    @Override
    public Optional<Order> findById(@NonNull UUID id) {
       return jpaOrderRepository.findById(id);
    }

    @Override
    public Order save(@NonNull Order order) {
        return jpaOrderRepository.save(order);
    }

    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll();
    }

    @Override
    public List<Order> findAll(int limit) {
        return jpaOrderRepository.findAll(PageRequest.of(0, limit)).getContent();
    }

    @Override
    public void update(@NonNull Order order) {
        jpaOrderRepository.save(order);
    }

    @Override
    public List<Order> findByCustomerIdAndProductIdAndStatus(String customerId, UUID productId, OrderStatus status) {
        List<Order> ordersByCustomerAndStatus = jpaOrderRepository.findByCustomerIdAndStatus(customerId, status);
        return ordersByCustomerAndStatus.stream()
                .filter(order -> order.items().stream()
                        .anyMatch(item -> item.productId().equals(productId)))
                .collect(Collectors.toList());
    }
    
}

