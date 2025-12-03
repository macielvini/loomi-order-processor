package com.loomi.order.infra.persistence.product;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loomi.order.domain.product.entity.Product;

public interface JpaProductRepository extends JpaRepository<Product, UUID> {
    
}
