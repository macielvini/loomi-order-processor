package com.loomi.order_processor.infra.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.lang.NonNull;

public class CorrelationIdInterceptor<K, V> implements RecordInterceptor<K, V> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public ConsumerRecord<K, V> intercept(@NonNull ConsumerRecord<K, V> record, @NonNull Consumer<K, V> consumer) {
        Header correlationHeader = record.headers().lastHeader(CORRELATION_ID_HEADER);
        if (correlationHeader != null) {
            String correlationId = new String(correlationHeader.value());
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        }
        return record;
    }

    @Override
    public void success(@NonNull ConsumerRecord<K, V> record, @NonNull Consumer<K, V> consumer) {
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }

    @Override
    public void failure(@NonNull ConsumerRecord<K, V> record, @NonNull Exception exception, @NonNull Consumer<K, V> consumer) {
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}

