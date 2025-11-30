package com.loomi.order_processor.infra.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-processed}")
    private String orderProcessedTopic;

    @Value("${kafka.topics.order-failed}")
    private String orderFailedTopic;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
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
    ProducerFactory<String, OrderCreatedEvent> orderCreatedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<String, OrderProcessedEvent> orderProcessedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    ProducerFactory<String, OrderFailedEvent> orderFailedProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
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
}
