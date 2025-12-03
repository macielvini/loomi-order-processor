package com.loomi.order.domain.product.entity;

import jakarta.persistence.*;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.loomi.order.domain.product.dto.RawProductMetadata;
import com.loomi.order.domain.product.dto.ProductType;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true, chain = true)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "product_type", nullable = false, length = 20)
	private ProductType productType;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "stock_quantity")
	private Integer stockQuantity;

	@Column(nullable = false, name = "is_active")
	@Builder.Default
	private Boolean isActive = true;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private RawProductMetadata metadata;

}

