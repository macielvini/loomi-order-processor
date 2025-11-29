package com.loomi.order_processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderProcessorApplicationTests {

	@MockitoBean
	private KafkaAdmin kafkaAdmin;

	@Test
	void contextLoads() {
	}

}
