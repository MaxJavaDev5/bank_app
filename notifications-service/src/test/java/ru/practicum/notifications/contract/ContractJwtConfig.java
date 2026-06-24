package ru.practicum.notifications.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class ContractJwtConfig {

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return token -> {
            if ("user-token".equals(token)) {
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject("user")
                        .claim("preferred_username", "user")
                        .claim("realm_access", Map.of("roles", List.of("USER")))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build();
            }
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("accounts-service")
                    .claim("realm_access", Map.of("roles", List.of("SERVICE")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        };
    }
}
