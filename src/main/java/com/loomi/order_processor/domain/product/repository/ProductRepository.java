package com.loomi.order_processor.domain.product.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.loomi.order_processor.domain.product.entity.Product;

public interface ProductRepository {

    Optional<Product> findById(UUID id);

    Product save(Product product);

    List<Product> findAll();

    void update(Product product);
}
