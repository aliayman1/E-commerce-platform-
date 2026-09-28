package com.microservices.pro.order;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

// Deliberately not @Configuration: a component-scanned interceptor would attach
// the service token to every Feign client, not just InventoryClient.
public class InventoryClientConfig {

    @Bean
    public RequestInterceptor clientCredentialsInterceptor(OrderServiceTokenClient tokenClient) {
        return template -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient.getAccessToken());
    }
}
