package com.loomi.order;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.SpringApplication;

@Disabled
public class TestOrderProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderProcessorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
