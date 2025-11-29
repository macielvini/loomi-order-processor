package com.loomi.order_processor.infra.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final JpaProductRepository jpaProductRepository;

    @Override
    public Optional<Product> findById(@NonNull UUID id) {
       return jpaProductRepository.findById(id);
    }

    @Override
    public Product save(@NonNull Product product) {
        return jpaProductRepository.save(product);
    }

    @Override
    public List<Product> findAll() {
        return jpaProductRepository.findAll();
    }

    @Override
    public List<Product> findAll(int limit) {
        return jpaProductRepository.findAll(PageRequest.of(0, limit)).getContent();
    }

    @Override
    public void update(@NonNull Product product) {
        jpaProductRepository.save(product);
    }
    
}
