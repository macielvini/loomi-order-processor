package com.loomi.order_processor;

import org.springframework.boot.SpringApplication;

public class TestOrderProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderProcessorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
