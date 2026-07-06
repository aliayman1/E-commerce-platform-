package com.microservices.pro.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    @Test
    void processPayment_returnsApproved_whenFailureRateIsZero() {
        PaymentService paymentService = new PaymentService(0);

        PaymentResponse response = paymentService.processPayment(new PaymentRequest(BigDecimal.TEN));

        assertEquals("APPROVED", response.status());
        assertEquals(BigDecimal.TEN, response.amount());
    }

    @Test
    void processPayment_throws_whenFailureRateIsHundred() {
        PaymentService paymentService = new PaymentService(100);

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(new PaymentRequest(BigDecimal.TEN)));
    }

    @Test
    void processPayment_generatesUniqueTransactionId_perCall() {
        PaymentService paymentService = new PaymentService(0);

        PaymentResponse first = paymentService.processPayment(new PaymentRequest(BigDecimal.TEN));
        PaymentResponse second = paymentService.processPayment(new PaymentRequest(BigDecimal.TEN));

        assertNotEquals(first.transactionId(), second.transactionId());
    }
}