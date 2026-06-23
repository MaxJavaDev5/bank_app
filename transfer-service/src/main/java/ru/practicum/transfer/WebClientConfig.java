package ru.practicum.transfer;

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

    @Value("${accounts.service.url:lb://accounts-service}")
    private String accountsServiceUrl;

    @Value("${accounts.service.timeout:3s}")
    private Duration accountsServiceTimeout;

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient accountsWebClient(
            WebClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultClientRegistrationId("transfer-client");

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(accountsServiceTimeout);

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(accountsServiceUrl)
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}
