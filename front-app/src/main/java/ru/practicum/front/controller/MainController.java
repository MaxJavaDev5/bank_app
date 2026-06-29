package ru.practicum.front.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.practicum.front.client.BankClient;
import ru.practicum.front.dto.AccountDto;
import ru.practicum.front.dto.AccountShortDto;
import ru.practicum.front.dto.CashAction;
import ru.practicum.front.dto.CashForm;
import ru.practicum.front.dto.TransferAccountDto;
import ru.practicum.front.dto.TransferForm;
import ru.practicum.front.dto.UpdateAccountForm;
import ru.practicum.front.model.RemoteException;

import java.math.BigDecimal;
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

    @GetMapping({"/cash", "/transfer"})
    public String redirectPostOnlyEndpoints() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) model.asMap().get("errors");
        String info = (String) model.asMap().get("info");
        return loadMainPage(model, errors, info);
    }

    @PostMapping("/account")
    public String editAccount(
            @Valid @ModelAttribute UpdateAccountForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser oidcUser) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", collectErrors(bindingResult));
            return "redirect:/account";
        }
        try {
            String[] nameParts = splitName(form.getName());
            bankClient.updateMyAccount(nameParts[1], nameParts[0], form.getBirthDate());
        } catch (Exception ex) {
            log.warn("Ошибка обновления профиля: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of(resolveErrorMessage(ex)));
        }
        return "redirect:/account";
    }

    @PostMapping("/cash")
    public String editCash(
            @Valid @ModelAttribute CashForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser oidcUser) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", collectErrors(bindingResult));
            return "redirect:/account";
        }
        try {
            BigDecimal amount = form.getAmount();
            if (form.getAction() == CashAction.DEPOSIT) {
                bankClient.deposit(amount);
                redirectAttributes.addFlashAttribute("info", "Положено " + amount + " руб.");
            } else {
                bankClient.withdraw(amount);
                redirectAttributes.addFlashAttribute("info", "Снято " + amount + " руб.");
            }
        } catch (Exception ex) {
            log.warn("Ошибка операции с наличными: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of(resolveErrorMessage(ex)));
        }
        return "redirect:/account";
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute TransferForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser oidcUser) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", collectErrors(bindingResult));
            return "redirect:/account";
        }
        try {
            bankClient.transfer(form.getToLogin(), form.getAmount());
            String recipientName = findAccountName(form.getToLogin());
            redirectAttributes.addFlashAttribute("info",
                    "Успешно переведено " + form.getAmount() + " руб клиенту " + recipientName);
        } catch (Exception ex) {
            log.warn("Ошибка перевода: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of(resolveErrorMessage(ex)));
        }
        return "redirect:/account";
    }

    @ExceptionHandler(WebClientResponseException.class)
    public String handleWebClientResponseException(
            WebClientResponseException exception,
            RedirectAttributes redirectAttributes) {
        if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
            String body = exception.getResponseBodyAsString();
            redirectAttributes.addFlashAttribute("errors",
                    List.of(!body.isBlank() ? body : "Операция отклонена: недостаточно прав"));
        } else {
            redirectAttributes.addFlashAttribute("errors",
                    List.of("Ошибка сервиса: HTTP " + exception.getStatusCode().value()));
        }
        return "redirect:/account";
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

    private List<String> collectErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
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
