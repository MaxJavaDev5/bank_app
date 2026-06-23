package ru.practicum.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.model.AccountsServiceUnavailableException;
import ru.practicum.transfer.model.RemoteException;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AccountsClient {

    private final WebClient accountsWebClient;

    public AccountsClient(WebClient accountsWebClient) {
        this.accountsWebClient = accountsWebClient;
    }

    @CircuitBreaker(name = "accountsService", fallbackMethod = "transferFallback")
    public TransferResponseDto transfer(String fromLogin, String toLogin, BigDecimal amount) {
        return accountsWebClient.post()
                .uri("/accounts/transfer")
                .bodyValue(Map.of(
                        "fromLogin", fromLogin,
                        "toLogin", toLogin,
                        "amount", amount
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RemoteException("accounts-service", body)
                                ))
                )
                .bodyToMono(TransferResponseDto.class)
                .block();
    }

    private TransferResponseDto transferFallback(
            String fromLogin, String toLogin, BigDecimal amount, Throwable cause) {
        throw new AccountsServiceUnavailableException(cause);
    }
}
