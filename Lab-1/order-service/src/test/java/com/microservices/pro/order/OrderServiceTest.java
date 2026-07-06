package com.microservices.pro.order;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        // Smaller/faster thresholds so the tests don't need dozens of calls to prove the behaviour
        "resilience4j.circuitbreaker.instances.paymentService.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.paymentService.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.paymentService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.paymentService.wait-duration-in-open-state=1s",
        "resilience4j.retry.instances.paymentService.max-attempts=2",
        "resilience4j.retry.instances.paymentService.wait-duration=50ms",
        "resilience4j.retry.instances.paymentService.enable-exponential-backoff=false"
})
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private PaymentClient paymentClient;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("paymentService").reset();
    }

    @Test
    void createOrder_returnsConfirmed_whenPaymentSucceeds() {
        when(paymentClient.processPayment(any()))
                .thenReturn(new PaymentResponse("APPROVED", "txn-1", BigDecimal.TEN));

        OrderResponse response = orderService.createOrder(new OrderRequest(BigDecimal.TEN));

        assertEquals("CONFIRMED", response.status());
    }

    @Test
    void createOrder_returnsPending_whenPaymentFailsConsistently() {
        when(paymentClient.processPayment(any()))
                .thenThrow(new RuntimeException("Payment Service unavailable"));

        OrderResponse response = orderService.createOrder(new OrderRequest(BigDecimal.TEN));

        assertEquals("PENDING", response.status());
        verify(paymentClient, times(2)).processPayment(any()); // max-attempts=2
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold() {
        when(paymentClient.processPayment(any()))
                .thenThrow(new RuntimeException("Payment Service unavailable"));

        // CircuitBreaker wraps Retry, so it records ONE outcome per createOrder() call
        // (after Retry exhausts its attempts) - 4 failed calls fills the sliding window.
        for (int i = 0; i < 4; i++) {
            orderService.createOrder(new OrderRequest(BigDecimal.TEN));
        }

        assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("paymentService").getState());
    }
}