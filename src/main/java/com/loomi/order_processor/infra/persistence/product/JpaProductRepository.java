package com.loomi.order_processor.infra.persistence.product;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loomi.order_processor.domain.product.entity.Product;

public interface JpaProductRepository extends JpaRepository<Product, UUID> {
    
}
