package com.loomi.order_processor.infra.e2e;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

class ProductRepositoryTestUtils {

    private final ProductRepository productRepository;

    ProductRepositoryTestUtils(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    Product createPhysicalProduct() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("warehouseLocation", "SP");

        Product product = Product.builder()
                .name("Physical Product")
                .productType(ProductType.PHYSICAL)
                .price(new BigDecimal("199.90"))
                .stockQuantity(100)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    Product createSubscriptionProductForMixedOrder() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("GROUP_ID", "SUB_GROUP_MIXED_1");

        Product product = Product.builder()
                .name("Subscription Product")
                .productType(ProductType.SUBSCRIPTION)
                .price(new BigDecimal("49.90"))
                .stockQuantity(0)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    Product createDigitalProductForMixedOrder() {
        RawProductMetadata metadata = new RawProductMetadata();

        Product product = Product.builder()
                .name("Digital Product")
                .productType(ProductType.DIGITAL)
                .price(new BigDecimal("29.90"))
                .stockQuantity(10)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    Product createPreOrderProductForMixedOrder() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("releaseDate", LocalDate.now().plusDays(30).toString());

        Product product = Product.builder()
                .name("Pre-order Product")
                .productType(ProductType.PRE_ORDER)
                .price(new BigDecimal("199.00"))
                .stockQuantity(50)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    Product createHighValuePhysicalProduct() {
        RawProductMetadata metadata = new RawProductMetadata();
        metadata.put("warehouseLocation", "SP");

        Product product = Product.builder()
                .name("High Value Physical Product")
                .productType(ProductType.PHYSICAL)
                .price(new BigDecimal("15000.00"))
                .stockQuantity(100)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }

    Product createCorporateProductForPendingApproval() {
        RawProductMetadata metadata = new RawProductMetadata();

        Product product = Product.builder()
                .name("Corporate High Value Product")
                .productType(ProductType.CORPORATE)
                .price(new BigDecimal("60000.00"))
                .stockQuantity(0)
                .isActive(true)
                .metadata(metadata)
                .build();

        return productRepository.save(product);
    }
}


