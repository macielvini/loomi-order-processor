package com.loomi.order_processor.domain.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.loomi.order_processor.app.utils.JsonUtils;

import lombok.Getter;

@Getter
public class HttpException extends RuntimeException {

    private final HttpStatus status;
    private final String error;
    private final LocalDateTime timestamp;

    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.error = message;
        this.timestamp = LocalDateTime.now();
    }

    public String toJson() {
        return JsonUtils.toJson(Map.of(
            "timestamp", timestamp,
            "status", status.value(),
            "error", error
        ));
    }
}
