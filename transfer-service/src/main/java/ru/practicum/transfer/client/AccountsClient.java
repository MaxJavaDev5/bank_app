package ru.practicum.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.practicum.transfer.dto.AccountDto;
import ru.practicum.transfer.model.OperationType;
import ru.practicum.transfer.model.RemoteException;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AccountsClient {

    private final WebClient accountsWebClient;

    public AccountsClient(WebClient accountsWebClient) {
        this.accountsWebClient = accountsWebClient;
    }

    @CircuitBreaker(name = "accountsService", fallbackMethod = "accountsFallback")
    public AccountDto withdraw(String login, BigDecimal amount) {
        return accountsWebClient.put()
                .uri("/accounts/{login}/balance", login)
                .bodyValue(Map.of(
                        "amount", amount,
                        "operationType", OperationType.WITHDRAW
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RemoteException("accounts-service", body)
                                ))
                )
                .bodyToMono(AccountDto.class)
                .block();
    }

    @CircuitBreaker(name = "accountsService", fallbackMethod = "accountsFallback")
    public AccountDto deposit(String login, BigDecimal amount) {
        return accountsWebClient.put()
                .uri("/accounts/{login}/balance", login)
                .bodyValue(Map.of(
                        "amount", amount,
                        "operationType", OperationType.DEPOSIT
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RemoteException("accounts-service", body)
                                ))
                )
                .bodyToMono(AccountDto.class)
                .block();
    }

    private AccountDto accountsFallback(String login, BigDecimal amount, Throwable cause) {
        throw new RuntimeException("Accounts service is not available");
    }
}
