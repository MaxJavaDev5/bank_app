package ru.practicum.accounts;

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

    @Value("${notifications.service.url:http://notifications-service:8084}")
    private String notificationsServiceUrl;

    @Value("${notifications.service.connect-timeout:3s}")
    private Duration notificationsServiceConnectTimeout;

    @Value("${notifications.service.timeout:5s}")
    private Duration notificationsServiceTimeout;

    @Bean
    public WebClient notificationsWebClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultClientRegistrationId("accounts-client");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) notificationsServiceConnectTimeout.toMillis())
                .responseTimeout(notificationsServiceTimeout);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(notificationsServiceUrl)
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}
