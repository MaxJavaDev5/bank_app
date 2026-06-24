package ru.practicum.front.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.practicum.front.dto.AccountDto;
import ru.practicum.front.dto.AccountShortDto;
import ru.practicum.front.model.RemoteException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// все запросы на бэкенд идут через gateway
@Component
public class BankClient {

    private final WebClient gatewayWebClient;

    public BankClient(WebClient gatewayWebClient) {
        this.gatewayWebClient = gatewayWebClient;
    }

    public AccountDto getMyAccount() {
        return gatewayWebClient.get()
                .uri("/api/accounts/me")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RemoteException("gateway", body)))
                )
                .bodyToMono(AccountDto.class)
                .block();
    }

    public AccountDto updateMyAccount(String firstName, String lastName, java.time.LocalDate birthDate) {
        return gatewayWebClient.put()
                .uri("/api/accounts/me")
                .bodyValue(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "birthDate", birthDate != null ? birthDate.toString() : ""
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RemoteException("gateway", body)))
                )
                .bodyToMono(AccountDto.class)
                .block();
    }

    public List<AccountShortDto> getAllAccounts() {
        return gatewayWebClient.get()
                .uri("/api/accounts")
                .retrieve()
                .bodyToFlux(AccountShortDto.class)
                .collectList()
                .block();
    }

    public AccountDto deposit(BigDecimal amount) {
        return gatewayWebClient.post()
                .uri("/api/cash/deposit")
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RemoteException("gateway", body)))
                )
                .bodyToMono(AccountDto.class)
                .block();
    }

    public AccountDto withdraw(BigDecimal amount) {
        return gatewayWebClient.post()
                .uri("/api/cash/withdraw")
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RemoteException("gateway", body)))
                )
                .bodyToMono(AccountDto.class)
                .block();
    }

    public void transfer(String toLogin, BigDecimal amount) {
        gatewayWebClient.post()
                .uri("/api/transfer")
                .bodyValue(Map.of(
                        "toLogin", toLogin,
                        "amount", amount
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RemoteException("gateway", body)))
                )
                .bodyToMono(Void.class)
                .block();
    }
}
