package com.microservices.pro.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private final Random random = new Random();
    private final int failureRate;

    public PaymentService(@Value("${payment.failure-rate:50}") int failureRate) {
        this.failureRate = failureRate;
    }

    // Simulates a configurable failure rate for testing resilience patterns downstream
    public PaymentResponse processPayment(PaymentRequest request) {
        if (random.nextInt(100) < failureRate) {
            throw new RuntimeException("Payment Service unavailable");
        }
        return new PaymentResponse("APPROVED", UUID.randomUUID().toString(), request.amount());
    }

    // Saga step: process payment for an order (local transaction)
    public String processPayment(String orderId) {
        if (random.nextInt(100) < failureRate) {
            throw new PaymentException("Payment declined for order " + orderId);
        }
        return UUID.randomUUID().toString();
    }
}