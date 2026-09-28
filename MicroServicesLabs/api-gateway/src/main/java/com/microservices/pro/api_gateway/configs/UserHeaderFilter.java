package com.microservices.pro.api_gateway.configs;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {

    private static final List<String> APPLICATION_ROLES = List.of("ADMIN", "CUSTOMER");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(principal -> ((JwtAuthenticationToken) principal).getToken())
                .map(jwt -> withUserHeaders(exchange, jwt))
                .switchIfEmpty(Mono.fromSupplier(() -> withUserHeaders(exchange, null)))
                .flatMap(chain::filter);
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest enriched = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    if (jwt != null) {
                        headers.add("X-User-Id", jwt.getSubject());
                        applicationRole(jwt).ifPresent(role -> headers.add("X-User-Role", role));
                    }
                }).build();
        return exchange.mutate().request(enriched).build();
    }

    private Optional<String> applicationRole(Jwt jwt) {
        List<String> roles = KeycloakRealmRoleConverter.realmRoles(jwt);
        return APPLICATION_ROLES.stream().filter(roles::contains).findFirst();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
