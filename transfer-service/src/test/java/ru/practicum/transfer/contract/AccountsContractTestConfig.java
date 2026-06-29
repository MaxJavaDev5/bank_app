package ru.practicum.transfer.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
public class AccountsContractTestConfig {

    @Bean
    WebClient accountsWebClient(StubFinder stubFinder) {
        String baseUrl = stubFinder.findStubUrl("ru.practicum", "accounts-service").toString();
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer service-token")
                .build();
    }
}
