package com.microservices.pro.api_gateway.configs;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "microservices-pro-course-secret-key-2024-minimum-256-bits";

    private JwtAuthFilter jwtAuthFilter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        jwtAuthFilter = new JwtAuthFilter(jwtUtil);

        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void protectedRouteWithoutToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/products").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, times(0)).filter(any());
    }

    @Test
    void protectedRouteWithValidToken_forwardsRequestWithEnrichedHeaders() {
        String token = validToken("user123", "CUSTOMER");
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/products")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, chain).block();

        var exchangeCaptor = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(exchangeCaptor.capture());

        ServerWebExchange forwarded = exchangeCaptor.getValue();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user123");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("CUSTOMER");
    }

    private String validToken(String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }
}