package com.loomi.order_processor.infra.notification;

import org.springframework.stereotype.Service;

import com.loomi.order_processor.domain.notification.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendTo(String email, Object payload) {
        log.info("Sending email to {} with payload: {}", email, payload);
    }
}

