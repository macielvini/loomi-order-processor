package com.loomi.order.infra.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.order.domain.order.entity.LowStockAlertEvent;
import com.loomi.order.domain.order.entity.OrderCreatedEvent;
import com.loomi.order.domain.order.entity.OrderFailedEvent;
import com.loomi.order.domain.order.entity.OrderPendingApprovalEvent;
import com.loomi.order.domain.order.entity.OrderProcessedEvent;

@Configuration
public class KafkaProducerConfig {
    private final KafkaProperties kafkaProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-processed}")
    private String orderProcessedTopic;

    @Value("${kafka.topics.order-failed}")
    private String orderFailedTopic;

    @Value("${kafka.topics.low-stock-alert}")
    private String lowStockAlertTopic;

    @Value("${kafka.topics.order-pending-approval}")
    private String orderPendingApprovalTopic;

    @Value("${kafka.topics.order-created-dlq:order-created-dlq}")
    private String orderCreatedDlqTopic;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new KafkaAdmin(configs);
    }

    @Bean
    NewTopic orderCreatedTopic() {
        return new NewTopic(orderCreatedTopic, 1, (short) 1);
    }

    @Bean
    NewTopic orderProcessedTopic() {
        return new NewTopic(orderProcessedTopic, 1, (short) 1);
    }

    @Bean
    NewTopic orderFailedTopic() {
        return new NewTopic(orderFailedTopic, 1, (short) 1);
    }

    @Bean
    NewTopic lowStockAlertTopic() {
        return new NewTopic(lowStockAlertTopic, 1, (short) 1);
    }

    @Bean
    NewTopic orderPendingApprovalTopic() {
        return new NewTopic(orderPendingApprovalTopic, 1, (short) 1);
    }

    @Bean
    NewTopic orderCreatedDlqTopic() {
        return new NewTopic(orderCreatedDlqTopic, 1, (short) 1);
    }

    @Bean
    ProducerFactory<String, OrderCreatedEvent> orderCreatedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<String, OrderProcessedEvent> orderProcessedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<String, OrderFailedEvent> orderFailedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<String, LowStockAlertEvent> lowStockAlertProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<String, OrderPendingApprovalEvent> orderPendingApprovalProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<Object, Object> genericProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
        return (ProducerFactory) factory;
    }

    @Bean
    KafkaTemplate<Object, Object> genericKafkaTemplate(
            ProducerFactory<Object, Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    KafkaTemplate<String, OrderCreatedEvent> orderCreatedKafkaTemplate(
            ProducerFactory<String, OrderCreatedEvent> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    KafkaTemplate<String, OrderProcessedEvent> orderProcessedKafkaTemplate(
            ProducerFactory<String, OrderProcessedEvent> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    KafkaTemplate<String, OrderFailedEvent> orderFailedKafkaTemplate(
            ProducerFactory<String, OrderFailedEvent> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    KafkaTemplate<String, LowStockAlertEvent> lowStockAlertKafkaTemplate(
            ProducerFactory<String, LowStockAlertEvent> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    KafkaTemplate<String, OrderPendingApprovalEvent> orderPendingApprovalKafkaTemplate(
            ProducerFactory<String, OrderPendingApprovalEvent> pf) {
        return new KafkaTemplate<>(pf);
    }
}
