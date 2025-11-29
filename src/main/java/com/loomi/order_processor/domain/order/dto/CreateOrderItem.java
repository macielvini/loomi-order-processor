package com.loomi.order_processor.domain.order.dto;

import java.util.UUID;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateOrderItem {
    @NotNull
    private UUID productId;
    
    @Min(1)
    private int quantity;
       
    @Length(max = 50000)
    private RawProductMetadata metadata;
}
