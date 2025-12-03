package com.loomi.order_processor.domain.notification.usecase;

public interface EmailService {
    void sendTo(String email, Object payload);
}

