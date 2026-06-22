package ru.practicum.notifications.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtUtils {

    private JwtUtils() {
    }

    public static String getLogin(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return jwt.getSubject();
    }
}
