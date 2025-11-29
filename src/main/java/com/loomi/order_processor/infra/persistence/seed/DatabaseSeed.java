package com.loomi.order_processor.infra.persistence.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import com.loomi.order_processor.domain.product.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Profile({"dev", "local"})
@Component
@Transactional
@RequiredArgsConstructor
public class DatabaseSeed implements CommandLineRunner {
    private final DataSource dataSource;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws SQLException {
        log.info("Seeding database...");
        seedProducts();
    }

    private void seedProducts() throws SQLException {
        if (productRepository.findAll(1).size() > 0) {
            log.info("Products table already populated, skipping...");
            return;
        }

        ClassPathResource script = new ClassPathResource("db/seed/products_seed.sql");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, script);
            log.info("Products table seeded successfully.");
        }
    }
}
