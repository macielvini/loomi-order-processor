package com.loomi.order.domain.product.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.loomi.order.domain.product.entity.Product;

public interface ProductRepository {

    Optional<Product> findById(UUID id);

    Product save(Product product);

    List<Product> findAll();

    List<Product> findAll(int limit);

    List<Product> findAllById(List<UUID> ids);

    void update(Product product);
}
