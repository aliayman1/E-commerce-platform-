package com.microservices.pro.api_gateway.configs;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    @Test
    void realmRoles_becomeRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("admin1")
                .claim("realm_access", Map.of("roles", List.of("offline_access", "ADMIN")))
                .build();

        assertThat(new KeycloakRealmRoleConverter().convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_offline_access", "ROLE_ADMIN");
    }
}
