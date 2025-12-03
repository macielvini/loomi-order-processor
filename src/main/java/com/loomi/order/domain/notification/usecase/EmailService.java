package com.loomi.order.domain.notification.usecase;

public interface EmailService {
    void sendTo(String email, Object payload);
}

