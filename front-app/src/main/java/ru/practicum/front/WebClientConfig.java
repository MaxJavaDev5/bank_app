package ru.practicum.front;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Value("${gateway.connect-timeout:3s}")
    private Duration gatewayConnectTimeout;

    @Value("${gateway.timeout:10s}")
    private Duration gatewayTimeout;

    public WebClientConfig(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Bean
    public WebClient gatewayWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) gatewayConnectTimeout.toMillis())
                .responseTimeout(gatewayTimeout);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(gatewayUrl)
                .filter(addAccessTokenHeader())
                .build();
    }

    private ExchangeFilterFunction addAccessTokenHeader() {
        return (request, next) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                        oauth2Token.getAuthorizedClientRegistrationId(),
                        oauth2Token.getName()
                );

                OAuth2AccessToken accessToken = authorizedClient != null
                        ? authorizedClient.getAccessToken()
                        : null;

                if (accessToken != null) {
                    ClientRequest requestWithToken = ClientRequest.from(request)
                            .header("Authorization", "Bearer " + accessToken.getTokenValue())
                            .build();
                    return next.exchange(requestWithToken);
                }
            }

            return next.exchange(request);
        };
    }
}
