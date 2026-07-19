package com.microservices.pro.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentClient(RestTemplate restTemplate,
                          @Value("${payment-service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject(paymentServiceUrl + "/api/v1/payments", request, PaymentResponse.class);
    }
}