package ru.practicum.front.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.practicum.front.client.BankClient;
import ru.practicum.front.dto.AccountDto;
import ru.practicum.front.dto.AccountShortDto;
import ru.practicum.front.dto.CashAction;
import ru.practicum.front.dto.TransferAccountDto;
import ru.practicum.front.model.RemoteException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final BankClient bankClient;

    public MainController(BankClient bankClient) {
        this.bankClient = bankClient;
    }

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        return loadMainPage(model, null, null);
    }

    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate,
            @AuthenticationPrincipal OidcUser oidcUser) {
        try {
            String[] nameParts = splitName(name);
            bankClient.updateMyAccount(nameParts[1], nameParts[0], birthdate);
            return loadMainPage(model, null, null);
        } catch (Exception ex) {
            log.warn("Ошибка обновления профиля: {}", ex.getMessage());
            return loadMainPage(model, List.of(resolveErrorMessage(ex)), null);
        }
    }

    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action,
            @AuthenticationPrincipal OidcUser oidcUser) {
        try {
            AccountDto account = bankClient.getMyAccount();
            BigDecimal amount = BigDecimal.valueOf(value);
            if (action == CashAction.PUT) {
                bankClient.deposit(account.getLogin(), amount);
                return loadMainPage(model, null, "Положено %d руб.".formatted(value));
            }
            bankClient.withdraw(account.getLogin(), amount);
            return loadMainPage(model, null, "Снято %d руб".formatted(value));
        } catch (Exception ex) {
            log.warn("Ошибка операции с наличными: {}", ex.getMessage());
            return loadMainPage(model, List.of(resolveErrorMessage(ex)), null);
        }
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login,
            @AuthenticationPrincipal OidcUser oidcUser) {
        try {
            AccountDto account = bankClient.getMyAccount();
            bankClient.transfer(account.getLogin(), login, BigDecimal.valueOf(value));
            String recipientName = findAccountName(login);
            return loadMainPage(model, null,
                    "Успешно переведено %d руб клиенту %s".formatted(value, recipientName));
        } catch (Exception ex) {
            log.warn("Ошибка перевода: {}", ex.getMessage());
            return loadMainPage(model, List.of(resolveErrorMessage(ex)), null);
        }
    }

    @ExceptionHandler(WebClientResponseException.class)
    public String handleWebClientResponseException(
            WebClientResponseException exception,
            Model model,
            @AuthenticationPrincipal OidcUser oidcUser) {
        if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
            String body = exception.getResponseBodyAsString();
            return loadMainPage(model,
                    List.of(!body.isBlank() ? body : "Операция отклонена: недостаточно прав"),
                    null);
        }
        return loadMainPage(model,
                List.of("Ошибка сервиса: HTTP " + exception.getStatusCode().value()),
                null);
    }

    private String loadMainPage(Model model, List<String> errors, String info) {
        try {
            AccountDto account = bankClient.getMyAccount();
            List<TransferAccountDto> accounts = bankClient.getAllAccounts().stream()
                    .filter(a -> !a.getLogin().equals(account.getLogin()))
                    .map(this::toTransferAccount)
                    .toList();

            model.addAttribute("name", formatName(account.getLastName(), account.getFirstName()));
            model.addAttribute("birthdate", account.getBirthDate() != null ? account.getBirthDate().toString() : "");
            model.addAttribute("sum", account.getBalance() != null ? account.getBalance().intValue() : 0);
            model.addAttribute("accounts", accounts);
            model.addAttribute("errors", errors);
            model.addAttribute("info", info);
        } catch (Exception ex) {
            log.warn("Ошибка загрузки главной страницы: {}", ex.getMessage());
            model.addAttribute("errors", List.of(resolveErrorMessage(ex)));
        }
        return "main";
    }

    private TransferAccountDto toTransferAccount(AccountShortDto account) {
        return new TransferAccountDto(
                account.getLogin(),
                formatName(account.getLastName(), account.getFirstName())
        );
    }

    private String findAccountName(String login) {
        return bankClient.getAllAccounts().stream()
                .filter(account -> login.equals(account.getLogin()))
                .findFirst()
                .map(account -> formatName(account.getLastName(), account.getFirstName()))
                .orElse(login);
    }

    private String formatName(String lastName, String firstName) {
        if (lastName != null && !lastName.isBlank() && firstName != null && !firstName.isBlank()) {
            return lastName + " " + firstName;
        }
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        if (lastName != null && !lastName.isBlank()) {
            return lastName;
        }
        return "";
    }

    // разбиваем ФИО на части - сначала фамилия потом имя
    private String[] splitName(String name) {
        String trimmed = name.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex < 0) {
            return new String[]{"", trimmed};
        }
        return new String[]{
                trimmed.substring(0, spaceIndex),
                trimmed.substring(spaceIndex + 1).trim()
        };
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex instanceof RemoteException remoteException) {
            return remoteException.getMessage();
        }
        if (ex instanceof WebClientResponseException webClientException) {
            String body = webClientException.getResponseBodyAsString();
            if (!body.isBlank()) {
                return body;
            }
        }
        return ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка";
    }
}
