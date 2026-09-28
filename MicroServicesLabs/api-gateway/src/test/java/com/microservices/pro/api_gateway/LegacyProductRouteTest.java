package com.microservices.pro.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.client.simple.instances[PRODUCT-SERVICE][0].uri=http://localhost:${wiremock.server.port}"
        })
@AutoConfigureWireMock(port = 0)
@AutoConfigureWebTestClient
class LegacyProductRouteTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void legacyPath_isRewrittenToV1_withDeprecationAndSunsetHeaders() {
        stubFor(get(urlEqualTo("/api/v1/products"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Deprecation", "true")
                .expectHeader().valueEquals("Sunset", "Wed, 01 Oct 2026 00:00:00 GMT");

        verify(getRequestedFor(urlEqualTo("/api/v1/products")));
    }

    @Test
    void legacyPath_writeWithoutToken_isRejectedBeforeReachingProductService() {
        webTestClient.post().uri("/api/products")
                .bodyValue("{\"name\":\"x\"}")
                .exchange()
                .expectStatus().isUnauthorized();

        verify(0, anyRequestedFor(urlMatching("/api/.*")));
    }
}
