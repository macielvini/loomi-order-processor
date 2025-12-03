package com.loomi.order.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.loomi.order.domain.product.dto.RawProductMetadata;
import com.loomi.order.domain.product.dto.ProductType;
import com.loomi.order.domain.product.entity.Product;
import com.loomi.order.domain.product.repository.ProductRepository;
import com.loomi.order.infra.persistence.product.ProductRepositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
@DataJpaTest
@Import({ProductRepositoryImpl.class})
class ProductRepositoryIntegrationTest {

        @Container
        static PostgreSQLContainer<?> postgres =  new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("order")
        .withUsername("appuser")
        .withPassword("apppass");

        @BeforeAll
        static void beforeAll() {
                postgres.start();
        }

        @AfterAll
        static void afterAll() {
                postgres.stop();
        }

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private ProductRepository productRepository;

        @Test
        void shouldFindProductById_whenProductExists() {
                Product product = Product.builder()
                                .name("Test Product")
                                .productType(ProductType.PHYSICAL)
                                .price(new BigDecimal("99.99"))
                                .stockQuantity(100)
                                .isActive(true)
                                .build();

                Product savedProduct = productRepository.save(product);
                UUID productId = savedProduct.id();

                Product foundProduct = productRepository.findById(productId).orElseThrow();

                assertThat(foundProduct.id()).isEqualTo(productId);
                assertThat(foundProduct.name()).isEqualTo("Test Product");
                assertThat(foundProduct.productType()).isEqualTo(ProductType.PHYSICAL);
                assertThat(foundProduct.price()).isEqualByComparingTo(new BigDecimal("99.99"));
                assertThat(foundProduct.stockQuantity()).isEqualTo(100);
                assertThat(foundProduct.isActive()).isTrue();
        }

        @Test
        void shouldReturnEmptyOptional_whenProductDoesNotExist() {
                UUID nonExistentId = UUID.randomUUID();

                Optional<Product> foundProduct = productRepository.findById(nonExistentId);

                assertThat(foundProduct).isEmpty();
        }

        @Test
        void shouldSaveProduct_successfully() {
                Product product = Product.builder()
                                .name("New Product")
                                .productType(ProductType.DIGITAL)
                                .price(new BigDecimal("49.99"))
                                .stockQuantity(50)
                                .isActive(true)
                                .build();

                Product savedProduct = productRepository.save(product);

                assertThat(savedProduct.id()).isNotNull();
                assertThat(savedProduct.name()).isEqualTo("New Product");
                assertThat(savedProduct.productType()).isEqualTo(ProductType.DIGITAL);
                assertThat(savedProduct.price()).isEqualByComparingTo(new BigDecimal("49.99"));
                assertThat(savedProduct.stockQuantity()).isEqualTo(50);
                assertThat(savedProduct.isActive()).isTrue();

                Product foundProduct = productRepository.findById(savedProduct.id()).orElseThrow();
                assertThat(foundProduct.name()).isEqualTo("New Product");
        }

        @Test
        void shouldSaveProduct_withMetadata() {
                RawProductMetadata metadata = new RawProductMetadata();
                metadata.put("category", "electronics");
                metadata.put("brand", "TestBrand");
                metadata.put("warranty", 12);

                Product product = Product.builder()
                                .name("Product with Metadata")
                                .productType(ProductType.PHYSICAL)
                                .price(new BigDecimal("199.99"))
                                .stockQuantity(25)
                                .isActive(true)
                                .metadata(metadata)
                                .build();

                Product savedProduct = productRepository.save(product);

                assertThat(savedProduct.metadata()).isNotNull();
                assertThat(savedProduct.metadata().get("category")).isEqualTo("electronics");
                assertThat(savedProduct.metadata().get("brand")).isEqualTo("TestBrand");
                assertThat(savedProduct.metadata().get("warranty")).isEqualTo(12);

                Product foundProduct = productRepository.findById(savedProduct.id()).orElseThrow();
                assertThat(foundProduct.metadata()).isNotNull();
                assertThat(foundProduct.metadata().get("category")).isEqualTo("electronics");
        }

        @Test
        void shouldGenerateId_whenSavingNewProduct() {
                Product product = Product.builder()
                                .name("Product Without ID")
                                .productType(ProductType.SUBSCRIPTION)
                                .price(new BigDecimal("29.99"))
                                .stockQuantity(0)
                                .isActive(true)
                                .build();

                assertThat(product.id()).isNull();

                Product savedProduct = productRepository.save(product);

                assertThat(savedProduct.id()).isNotNull();
                assertThat(savedProduct.id()).isInstanceOf(UUID.class);
        }

        @Test
        void shouldFindAllProducts_whenProductsExist() {
                Product product1 = Product.builder()
                                .name("Product 1")
                                .productType(ProductType.PHYSICAL)
                                .price(new BigDecimal("10.00"))
                                .stockQuantity(10)
                                .isActive(true)
                                .build();

                Product product2 = Product.builder()
                                .name("Product 2")
                                .productType(ProductType.DIGITAL)
                                .price(new BigDecimal("20.00"))
                                .stockQuantity(20)
                                .isActive(true)
                                .build();

                Product product3 = Product.builder()
                                .name("Product 3")
                                .productType(ProductType.PRE_ORDER)
                                .price(new BigDecimal("30.00"))
                                .stockQuantity(30)
                                .isActive(false)
                                .build();

                productRepository.save(product1);
                productRepository.save(product2);
                productRepository.save(product3);

                List<Product> allProducts = productRepository.findAll();

                assertThat(allProducts).hasSize(3);
                assertThat(allProducts).extracting(Product::name)
                                .containsExactlyInAnyOrder("Product 1", "Product 2", "Product 3");
        }

        @Test
        void shouldReturnEmptyList_whenNoProductsExist() {
                List<Product> allProducts = productRepository.findAll();

                assertThat(allProducts).isEmpty();
        }

        @Test
        void shouldUpdateProduct_successfully() {
                Product product = Product.builder()
                                .name("Original Name")
                                .productType(ProductType.PHYSICAL)
                                .price(new BigDecimal("100.00"))
                                .stockQuantity(100)
                                .isActive(true)
                                .build();

                Product savedProduct = productRepository.save(product);
                UUID productId = savedProduct.id();

                savedProduct.name("Updated Name")
                                .price(new BigDecimal("150.00"))
                                .stockQuantity(75)
                                .isActive(false)
                                .productType(ProductType.CORPORATE);

                productRepository.update(savedProduct);

                Product updatedProduct = productRepository.findById(productId).orElseThrow();

                assertThat(updatedProduct.name()).isEqualTo("Updated Name");
                assertThat(updatedProduct.price()).isEqualByComparingTo(new BigDecimal("150.00"));
                assertThat(updatedProduct.stockQuantity()).isEqualTo(75);
                assertThat(updatedProduct.isActive()).isFalse();
                assertThat(updatedProduct.productType()).isEqualTo(ProductType.CORPORATE);
        }

        @Test
        void shouldUpdateProduct_metadata() {
                RawProductMetadata originalMetadata = new RawProductMetadata();
                originalMetadata.put("original", "value");

                Product product = Product.builder()
                                .name("Product with Metadata")
                                .productType(ProductType.DIGITAL)
                                .price(new BigDecimal("50.00"))
                                .stockQuantity(50)
                                .isActive(true)
                                .metadata(originalMetadata)
                                .build();

                Product savedProduct = productRepository.save(product);
                UUID productId = savedProduct.id();

                RawProductMetadata updatedMetadata = new RawProductMetadata();
                updatedMetadata.put("updated", "new value");
                updatedMetadata.put("version", 2);

                savedProduct.metadata(updatedMetadata);
                productRepository.update(savedProduct);

                Product updatedProduct = productRepository.findById(productId).orElseThrow();

                assertThat(updatedProduct.metadata()).isNotNull();
                assertThat(updatedProduct.metadata().get("updated")).isEqualTo("new value");
                assertThat(updatedProduct.metadata().get("version")).isEqualTo(2);
                assertThat(updatedProduct.metadata().get("original")).isNull();
        }

        @Test
        void shouldFindAllProductsById_whenProductsExist() {
                Product product1 = Product.builder()
                                .name("Product 1")
                                .productType(ProductType.PHYSICAL)
                                .price(new BigDecimal("10.00"))
                                .stockQuantity(10)
                                .isActive(true)
                                .build();

                Product product2 = Product.builder()
                                .name("Product 2")
                                .productType(ProductType.DIGITAL)
                                .price(new BigDecimal("20.00"))
                                .stockQuantity(20)
                                .isActive(true)
                                .build();

                Product product3 = Product.builder()
                                .name("Product 3")
                                .productType(ProductType.SUBSCRIPTION)
                                .price(new BigDecimal("30.00"))
                                .stockQuantity(30)
                                .isActive(false)
                                .build();

                Product savedProduct1 = productRepository.save(product1);
                Product savedProduct2 = productRepository.save(product2);
                Product savedProduct3 = productRepository.save(product3);

                List<Product> foundProducts = productRepository.findAllById(
                                List.of(savedProduct1.id(), savedProduct2.id(), savedProduct3.id()));

                assertThat(foundProducts).hasSize(3);
                assertThat(foundProducts).extracting(Product::id)
                                .containsExactlyInAnyOrder(savedProduct1.id(), savedProduct2.id(), savedProduct3.id());
                assertThat(foundProducts).extracting(Product::name)
                                .containsExactlyInAnyOrder("Product 1", "Product 2", "Product 3");
        }

        @Test
        void shouldFindOnlyExistingProducts_whenSomeIdsDoNotExist() {
                Product product1 = Product.builder()
                                .name("Product 1")
                                .productType(ProductType.PHYSICAL)
                                .price(new BigDecimal("10.00"))
                                .stockQuantity(10)
                                .isActive(true)
                                .build();

                Product product2 = Product.builder()
                                .name("Product 2")
                                .productType(ProductType.DIGITAL)
                                .price(new BigDecimal("20.00"))
                                .stockQuantity(20)
                                .isActive(true)
                                .build();

                Product savedProduct1 = productRepository.save(product1);
                Product savedProduct2 = productRepository.save(product2);
                UUID nonExistentId = UUID.randomUUID();

                List<Product> foundProducts = productRepository.findAllById(
                                List.of(savedProduct1.id(), savedProduct2.id(), nonExistentId));

                assertThat(foundProducts).hasSize(2);
                assertThat(foundProducts).extracting(Product::id)
                                .containsExactlyInAnyOrder(savedProduct1.id(), savedProduct2.id());
        }

        @Test
        void shouldReturnEmptyList_whenNoIdsMatch() {
                UUID nonExistentId1 = UUID.randomUUID();
                UUID nonExistentId2 = UUID.randomUUID();

                List<Product> foundProducts = productRepository.findAllById(
                                List.of(nonExistentId1, nonExistentId2));

                assertThat(foundProducts).isEmpty();
        }

        @Test
        void shouldReturnEmptyList_whenEmptyIdListProvided() {
                List<Product> foundProducts = productRepository.findAllById(List.of());

                assertThat(foundProducts).isEmpty();
        }
}
