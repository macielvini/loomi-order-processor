package com.loomi.order_processor.domain.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true, chain = true)
@Getter(onMethod_ = @JsonProperty)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OrderItem {
    @NotNull
    private UUID productId;

    private String customerId;
    
    @Min(1)
    private int quantity;

    @NotNull
    private ProductType productType;
    
    @NotNull
    @Positive
    private BigDecimal price;
    
    private RawProductMetadata metadata;

    @JsonIgnore
    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public static OrderItem fromProduct(Product product, Integer quantity, RawProductMetadata metadata) {
        return OrderItem.builder()
            .productId(product.id())
            .quantity(quantity)
            .productType(product.productType())
            .price(product.price())
            .metadata(metadata)
            .build();
    }

    public static OrderItem fromProduct(Product product, String customerId, Integer quantity, RawProductMetadata metadata) {
        return OrderItem.builder()
            .productId(product.id())
            .customerId(customerId)
            .quantity(quantity)
            .productType(product.productType())
            .price(product.price())
            .metadata(metadata)
            .build();
    }
}
