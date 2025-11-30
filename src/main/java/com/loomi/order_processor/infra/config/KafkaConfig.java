package com.loomi.order_processor.infra.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.order_processor.domain.order.entity.OrderEvent;

@Configuration
@Profile("local")
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.order-events}")
    private String orderTopic;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    NewTopic orderTopic() {
        return new NewTopic(orderTopic, 1, (short) 1);
    }

    @Bean
    ProducerFactory<String, Object> producerFactory(ObjectMapper om) {
        var config = new HashMap<String, Object>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new JsonSerializer<>(om));
    }

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper om) {
        return new KafkaTemplate<>(producerFactory(om));
    }

    @Bean
    ConsumerFactory<String, OrderEvent<?>> orderEventConsumerFactory(ObjectMapper om) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<OrderEvent<?>> deserializer = new JsonDeserializer<>(OrderEvent.class, om);

        deserializer.addTrustedPackages("com.loomi.order_processor");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrderEvent<?>> kafkaListenerContainerFactory(ObjectMapper om) {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent<?>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory(om));
        return factory;
    }
}
