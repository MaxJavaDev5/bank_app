package ru.practicum.cash;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${accounts.service.url:http://accounts-service:8081}")
    private String accountsServiceUrl;

    @Value("${accounts.service.connect-timeout:3s}")
    private Duration accountsServiceConnectTimeout;

    @Value("${accounts.service.timeout:5s}")
    private Duration accountsServiceTimeout;

    @Bean
    public WebClient accountsWebClient(WebClient.Builder webClientBuilder,
                                       OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultClientRegistrationId("cashclient");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) accountsServiceConnectTimeout.toMillis())
                .responseTimeout(accountsServiceTimeout);

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(accountsServiceUrl)
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}
