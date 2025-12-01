package com.loomi.order_processor.domain.notification.service;

public interface EmailService {
    void sendTo(String email, Object payload);
}

