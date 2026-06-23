package ru.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.AccountShortDto;
import ru.practicum.accounts.dto.TransferRequestDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateAccountDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.security.JwtUtils;
import ru.practicum.accounts.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public AccountDto getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getAccountByLogin(JwtUtils.getLogin(jwt));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public AccountDto updateMyAccount(
            @Valid @RequestBody UpdateAccountDto updateDto,
            @AuthenticationPrincipal Jwt jwt) {
        return accountService.updateAccount(JwtUtils.getLogin(jwt), updateDto);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<AccountShortDto> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PutMapping("/{login}/balance")
    @PreAuthorize("hasRole('SERVICE')")
    public AccountDto updateBalance(
            @PathVariable String login,
            @Valid @RequestBody UpdateBalanceDto updateBalanceDto) {
        return accountService.updateBalance(login, updateBalanceDto);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('SERVICE')")
    public TransferResponseDto transfer(@Valid @RequestBody TransferRequestDto transferDto) {
        return accountService.transfer(
                transferDto.getFromLogin(),
                transferDto.getToLogin(),
                transferDto.getAmount()
        );
    }
}
