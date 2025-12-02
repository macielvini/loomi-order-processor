package com.loomi.order_processor.app.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "order-processing")
public class OrderProcessingConfig {

    private BigDecimal highValueThreshold;
    private BigDecimal fraudThreshold;

}
