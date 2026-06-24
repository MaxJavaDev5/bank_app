package ru.practicum.cash.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
public class AccountsContractTestConfig {

    @Bean
    WebClient accountsWebClient() {
        return WebClient.builder()
                .baseUrl("http://127.0.0.1:18081")
                .defaultHeader("Authorization", "Bearer service-token")
                .build();
    }
}
