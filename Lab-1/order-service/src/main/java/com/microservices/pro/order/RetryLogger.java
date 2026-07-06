package com.microservices.pro.order;

import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RetryLogger {

    private static final Logger log = LoggerFactory.getLogger(RetryLogger.class);

    private final RetryRegistry retryRegistry;

    public RetryLogger(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void attachRetryListeners() {
        retryRegistry.retry("paymentService").getEventPublisher()
                .onRetry(e -> log.warn("[RETRY] Attempt #{} - {}",
                        e.getNumberOfRetryAttempts(), e.getLastThrowable().getMessage()))
                .onSuccess(e -> log.info("[RETRY] Succeeded after {} attempt(s)",
                        e.getNumberOfRetryAttempts()));
    }
}