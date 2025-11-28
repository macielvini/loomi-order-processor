package com.loomi.order_processor.infra.web.controllers;

import com.loomi.order_processor.app.dto.HealthResponse;
import com.loomi.order_processor.app.service.HealthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping("/health")
	public ResponseEntity<HealthResponse> health() {
		return ResponseEntity.ok(new HealthResponse(healthService.getHealthStatus()));
	}
}

