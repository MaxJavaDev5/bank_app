package ru.practicum.front;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {

    private final OidcClientInitiatedLogoutSuccessHandler delegate;

    public KeycloakLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        this.delegate.setPostLogoutRedirectUri("{baseUrl}");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        delegate.onLogoutSuccess(request, response, authentication);
    }
}
