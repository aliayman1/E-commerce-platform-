package com.microservices.pro.order;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
        "resilience4j.retry.instances.paymentService.enable-exponential-backoff=false",
        "resilience4j.bulkhead.instances.paymentService.max-concurrent-calls=1",
        "resilience4j.bulkhead.instances.paymentService.max-wait-duration=0",
        "resilience4j.timelimiter.instances.paymentService.timeout-duration=2s",
        "resilience4j.timelimiter.instances.paymentService.cancel-running-future=true"
})
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private InventoryClient inventoryClient;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("paymentService").reset();
        // Default: stock is available unless a test overrides this stub
        when(inventoryClient.checkStock(anyString(), anyInt()))
                .thenReturn(new StockCheckResponse("PROD-001", 1, true, 99));
    }

    @Test
    void createOrder_returnsConfirmed_whenPaymentSucceeds() throws Exception {
        when(paymentClient.processPayment(any()))
                .thenReturn(new PaymentResponse("APPROVED", "txn-1", BigDecimal.TEN));

        OrderResponse response = orderService.createOrderAsync(new OrderRequest("PROD-001", 1, BigDecimal.TEN))
                .get(2, TimeUnit.SECONDS);

        assertEquals("CONFIRMED", response.status());
    }

    @Test
    void createOrder_returnsRejected_whenInventoryReportsInsufficientStock() throws Exception {
        when(inventoryClient.checkStock("PROD-003", 1))
                .thenThrow(new InsufficientStockException("Product out of stock"));

        OrderResponse response = orderService.createOrderAsync(new OrderRequest("PROD-003", 1, BigDecimal.TEN))
                .get(2, TimeUnit.SECONDS);

        assertEquals("REJECTED", response.status());
        assertEquals("Product out of stock", response.message());
        verify(paymentClient, never()).processPayment(any());
    }

    @Test
    void createOrder_checksInventoryBeforeCallingPayment() throws Exception {
        when(paymentClient.processPayment(any()))
                .thenReturn(new PaymentResponse("APPROVED", "txn-4", BigDecimal.TEN));

        orderService.createOrderAsync(new OrderRequest("PROD-001", 2, BigDecimal.TEN))
                .get(2, TimeUnit.SECONDS);

        verify(inventoryClient, times(1)).checkStock("PROD-001", 2);
        verify(paymentClient, times(1)).processPayment(any());
    }

    @Test
    void createOrder_returnsPending_whenPaymentFailsConsistently() throws Exception {
        when(paymentClient.processPayment(any()))
                .thenThrow(new RuntimeException("Payment Service unavailable"));

        OrderResponse response = orderService.createOrderAsync(new OrderRequest("PROD-001", 1, BigDecimal.TEN))
                .get(2, TimeUnit.SECONDS);

        assertEquals("PENDING", response.status());
        verify(paymentClient, times(2)).processPayment(any()); // max-attempts=2
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold() throws Exception {
        when(paymentClient.processPayment(any()))
                .thenThrow(new RuntimeException("Payment Service unavailable"));

        // CircuitBreaker wraps Retry, so it records ONE outcome per createOrderAsync() call
        // (after Retry exhausts its attempts) - 4 failed calls fills the sliding window.
        for (int i = 0; i < 4; i++) {
            orderService.createOrderAsync(new OrderRequest("PROD-001", 1, BigDecimal.TEN)).get(2, TimeUnit.SECONDS);
        }

        assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker("paymentService").getState());
    }

    @Test
    void bulkhead_returnsQueued_whenConcurrentLimitExceeded() throws Exception {
        CountDownLatch releaseLatch = new CountDownLatch(1);
        when(paymentClient.processPayment(any())).thenAnswer(invocation -> {
            releaseLatch.await(3, TimeUnit.SECONDS);
            return new PaymentResponse("APPROVED", "txn-2", BigDecimal.TEN);
        });

        // First call occupies the only bulkhead slot (max-concurrent-calls=1)
        CompletableFuture<OrderResponse> first = orderService.createOrderAsync(new OrderRequest("PROD-001", 1, BigDecimal.TEN));
        Thread.sleep(200); // let the first call acquire the bulkhead permit

        OrderResponse second = orderService.createOrderAsync(new OrderRequest("PROD-001", 1, BigDecimal.TEN))
                .get(2, TimeUnit.SECONDS);
        assertEquals("QUEUED", second.status());

        releaseLatch.countDown();
        OrderResponse firstResult = first.get(2, TimeUnit.SECONDS);
        assertEquals("CONFIRMED", firstResult.status());
    }

    @Test
    void timeLimiter_returnsPending_whenPaymentExceedsTimeout() throws Exception {
        // A background thread (not the TimeLimiter's cancelled future) still holds the Bulkhead
        // permit until this mocked call actually returns, so release it via a latch instead of a
        // fixed sleep - otherwise the permit would leak into the next test.
        CountDownLatch releaseLatch = new CountDownLatch(1);
        when(paymentClient.processPayment(any())).thenAnswer(invocation -> {
            releaseLatch.await(5, TimeUnit.SECONDS); // slower than the 2s TimeLimiter
            return new PaymentResponse("APPROVED", "txn-3", BigDecimal.TEN);
        });

        OrderResponse response = orderService.createOrderAsync(new OrderRequest("PROD-001", 1, BigDecimal.TEN))
                .get(4, TimeUnit.SECONDS);

        assertEquals("PENDING", response.status());
        assertEquals("Payment timed out", response.message());

        releaseLatch.countDown();
        Thread.sleep(100); // let the background call finish and release the Bulkhead permit
    }
}