package com.microservices.pro.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Component
public class OrderServiceTokenClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceTokenClient.class);
    private static final long EXPIRY_MARGIN_SECONDS = 30;

    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public OrderServiceTokenClient(@Value("${keycloak.token-uri}") String tokenUri,
                                   @Value("${keycloak.client-id}") String clientId,
                                   @Value("${keycloak.client-secret}") String clientSecret) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<?, ?> response = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        cachedToken = (String) response.get("access_token");
        long expiresIn = ((Number) response.get("expires_in")).longValue();
        expiresAt = Instant.now().plusSeconds(expiresIn - EXPIRY_MARGIN_SECONDS);
        log.info("[CC] Fetched new client-credentials token for {}, expires in {}s", clientId, expiresIn);
        return cachedToken;
    }
}
