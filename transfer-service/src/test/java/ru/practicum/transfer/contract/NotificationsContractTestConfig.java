package ru.practicum.transfer.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
public class NotificationsContractTestConfig {

    @Bean
    WebClient notificationsWebClient() {
        return WebClient.builder()
                .baseUrl("http://127.0.0.1:18084")
                .defaultHeader("Authorization", "Bearer test-token")
                .build();
    }
}
