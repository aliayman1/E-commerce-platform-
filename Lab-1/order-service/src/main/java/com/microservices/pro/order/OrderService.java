package com.microservices.pro.order;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
    @TimeLimiter(name = "paymentService", fallbackMethod = "timeoutFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService") // Retry wraps INSIDE CircuitBreaker
    public CompletableFuture<OrderResponse> createOrderAsync(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            PaymentResponse payment = paymentClient.processPayment(new PaymentRequest(request.amount()));
            return new OrderResponse("CONFIRMED", payment.transactionId());
        });
    }

    // Fallback: MUST match original params + Throwable as last param
    public CompletableFuture<OrderResponse> paymentFallback(OrderRequest request, Throwable ex) {
        log.warn("Payment failed, returning PENDING. Reason: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse("PENDING", "Will retry payment"));
    }

    // TimeLimiter fallback — called on TimeoutException
    public CompletableFuture<OrderResponse> timeoutFallback(OrderRequest request, TimeoutException ex) {
        log.warn("[TIMEOUT] Payment exceeded 2s limit: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse("PENDING", "Payment timed out"));
    }

    // Bulkhead fallback — called when concurrent limit exceeded
    public CompletableFuture<OrderResponse> bulkheadFallback(OrderRequest request, BulkheadFullException ex) {
        log.warn("[BULKHEAD] Concurrent limit reached: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse("QUEUED", "System busy — your order is queued"));
    }
}