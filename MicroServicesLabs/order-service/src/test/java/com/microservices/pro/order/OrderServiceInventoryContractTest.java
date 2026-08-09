package com.microservices.pro.order;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "inventory-service", port = "8888", pactVersion = PactSpecVersion.V3)
class OrderServiceInventoryContractTest {

    @Pact(consumer = "order-service", provider = "inventory-service")
    RequestResponsePact checkStockAvailable(PactDslWithProvider builder) {
        return builder
                .given("PROD-001 has 100 units in stock")
                .uponReceiving("a stock check for PROD-001 quantity 5")
                .path("/api/v1/inventory/check").method("GET")
                .query("productId=PROD-001&quantity=5")
                .willRespondWith()
                .status(200)
                .body(LambdaDsl.newJsonBody(body -> body
                        .booleanValue("available", true)
                        .integerType("remainingStock", 95)
                ).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "checkStockAvailable")
    void checkStock_deserializesAvailableField_correctly(MockServer mockServer) {

        InventoryClient client = buildFeignClient(mockServer.getUrl());

        StockCheckResponse response = client.checkStock("PROD-001", 5);

        assertThat(response.available()).isTrue();
        assertThat(response.remainingStock()).isGreaterThan(0);
    }

    private InventoryClient buildFeignClient(String baseUrl) {
        return Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(new JacksonDecoder())
                .target(InventoryClient.class, baseUrl + "/api/v1/inventory");
    }
}