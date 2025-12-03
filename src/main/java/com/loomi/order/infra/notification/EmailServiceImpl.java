package com.loomi.order.infra.notification;

import org.springframework.stereotype.Service;

import com.loomi.order.domain.notification.usecase.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendTo(String email, Object payload) {
        log.info("Sending email to {} with payload: {}", email, payload);
    }
}

