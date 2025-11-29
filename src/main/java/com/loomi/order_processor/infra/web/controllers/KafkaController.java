package com.loomi.order_processor.infra.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loomi.order_processor.infra.producer.KafkaProducerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class KafkaController {
    private final KafkaProducerService producer;

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestParam String topic,
                                       @RequestParam(required = false) String key,
                                       @RequestBody(required = false) String body) {
        String payload = body == null ? "" : body;
        if (key != null && !key.isBlank()) producer.send(topic, key, payload);
        else producer.send(topic, payload);
        return ResponseEntity.accepted().body("Enviado");
    }
}
