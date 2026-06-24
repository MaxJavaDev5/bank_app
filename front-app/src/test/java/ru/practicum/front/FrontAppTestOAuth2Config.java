package ru.practicum.front;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@TestConfiguration
public class FrontAppTestOAuth2Config {

    private static final String ISSUER = "http://localhost:8180/realms/bank-realm";

    @Bean
    @Primary
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("bank-ui")
                .clientId("bank-ui")
                .clientSecret("front-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8085/login/oauth2/code/bank-ui")
                .authorizationUri(ISSUER + "/protocol/openid-connect/auth")
                .tokenUri(ISSUER + "/protocol/openid-connect/token")
                .userInfoUri(ISSUER + "/protocol/openid-connect/userinfo")
                .jwkSetUri(ISSUER + "/protocol/openid-connect/certs")
                .userNameAttributeName("preferred_username")
                .clientName("bank-ui")
                .scope("openid", "profile")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
