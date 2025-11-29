package com.loomi.order_processor.infra.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;


	@Value("${kafka.topics.order-events}")
	private String orderEventsTopic;

	@Bean 
	KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
	}

	@Bean
	NewTopic orderEventsTopic() {
		return new NewTopic(orderEventsTopic, 1, (short) 1);
	}

}

