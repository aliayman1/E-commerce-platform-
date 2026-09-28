package com.microservices.pro.api_gateway.configs;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserHeaderFilterTest {

    @Test
    void authenticatedRequest_forwardsUserIdAndRoleFromKeycloakJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("e90aeb2a-80ba-44e3-b2ef-200c755c73e1")
                .claim("realm_access", Map.of("roles", List.of("offline_access", "CUSTOMER")))
                .build();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/stock-check")
                .header("X-User-Id", "spoofed")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request).mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt)))
                .build();
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        new UserHeaderFilter().filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> forwarded = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(forwarded.capture());
        assertThat(forwarded.getValue().getRequest().getHeaders().get("X-User-Id"))
                .containsExactly("e90aeb2a-80ba-44e3-b2ef-200c755c73e1");
        assertThat(forwarded.getValue().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("CUSTOMER");
    }
}
