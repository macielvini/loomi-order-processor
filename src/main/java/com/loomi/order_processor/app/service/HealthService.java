package com.loomi.order_processor.app.service;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {

	public Instant getHealthStatus() {
		return Instant.now();
	}
}

