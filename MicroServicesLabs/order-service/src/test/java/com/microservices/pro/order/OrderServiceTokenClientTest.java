package com.microservices.pro.order;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTokenClientTest {

    private WireMockServer keycloak;

    @BeforeEach
    void setUp() {
        keycloak = new WireMockServer(options().dynamicPort());
        keycloak.start();
    }

    @AfterEach
    void tearDown() {
        keycloak.stop();
    }

    @Test
    void getAccessToken_requestsClientCredentialsOnce_andReusesCachedToken() {
        keycloak.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"service-token\",\"expires_in\":300}")));
        OrderServiceTokenClient tokenClient = new OrderServiceTokenClient(
                keycloak.baseUrl() + "/token", "order-service", "secret");

        String first = tokenClient.getAccessToken();
        String second = tokenClient.getAccessToken();

        assertThat(first).isEqualTo("service-token");
        assertThat(second).isEqualTo("service-token");
        keycloak.verify(1, postRequestedFor(urlEqualTo("/token"))
                .withRequestBody(containing("grant_type=client_credentials")));
    }
}
