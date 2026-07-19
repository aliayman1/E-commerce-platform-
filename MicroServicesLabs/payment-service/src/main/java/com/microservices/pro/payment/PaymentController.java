package com.microservices.pro.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${payment.delay-ms:0}")
    private long delayMs;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request)
            throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs); // simulate slow service
        }
        return ResponseEntity.ok(paymentService.processPayment(request));
    }
}