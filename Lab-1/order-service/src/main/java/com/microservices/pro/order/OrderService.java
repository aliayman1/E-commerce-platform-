package com.microservices.pro.order;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService") // Retry wraps INSIDE CircuitBreaker
    public OrderResponse createOrder(OrderRequest request) {
        PaymentResponse payment = paymentClient.processPayment(new PaymentRequest(request.amount()));
        return new OrderResponse("CONFIRMED", payment.transactionId());
    }

    // Fallback: MUST match original params + Throwable as last param
    public OrderResponse paymentFallback(OrderRequest request, Throwable ex) {
        log.warn("Payment failed, returning PENDING. Reason: {}", ex.getMessage());
        return new OrderResponse("PENDING", "Will retry payment");
    }
}