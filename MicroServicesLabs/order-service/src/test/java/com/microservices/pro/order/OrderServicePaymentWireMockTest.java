package com.microservices.pro.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "payment-service.url=http://localhost:${wiremock.server.port}")
@AutoConfigureWireMock(port = 0)
class OrderServicePaymentWireMockTest {

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_returnsConfirmed_whenPaymentApproved() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\":\"TXN-001\",\"status\":\"APPROVED\"}")));

        PaymentOrderResponse response = orderService.createOrderWithPayment(
                new OrderRequest("PROD-001", 1, new BigDecimal("100.00"), "cust-1"));

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.transactionId()).isEqualTo("TXN-001");
    }

    @Test
    void createOrder_returnsPending_whenPaymentServiceUnavailable() {

        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse().withStatus(503)));

        PaymentOrderResponse response = orderService.createOrderWithPayment(
                new OrderRequest("PROD-001", 1, new BigDecimal("100.00"), "cust-1"));

        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void createOrder_sendsCorrectPayload_toPaymentService() {

        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\":\"TXN-002\",\"status\":\"APPROVED\",\"amount\":250.00}")));

        orderService.createOrderWithPayment(
                new OrderRequest("PROD-002", 2, new BigDecimal("250.00"), "cust-1"));

        verify(postRequestedFor(urlEqualTo("/api/v1/payments"))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("250.0"))));
    }
}