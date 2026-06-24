package ru.practicum.accounts;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
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

    @Value("${notifications.service.url:lb://notifications-service}")
    private String notificationsServiceUrl;

    @Value("${notifications.service.connect-timeout:3s}")
    private Duration notificationsServiceConnectTimeout;

    @Value("${notifications.service.timeout:5s}")
    private Duration notificationsServiceTimeout;

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient notificationsWebClient(
            WebClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultClientRegistrationId("accounts-client");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) notificationsServiceConnectTimeout.toMillis())
                .responseTimeout(notificationsServiceTimeout);

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(notificationsServiceUrl)
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}
