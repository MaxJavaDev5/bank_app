package ru.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;
import ru.practicum.cash.security.JwtUtils;
import ru.practicum.cash.service.CashService;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public AccountDto deposit(
            @Valid @RequestBody CashOperationDto operationDto,
            JwtAuthenticationToken authentication) {
        String login = JwtUtils.getLogin(authentication.getToken());
        return cashService.deposit(login, operationDto);
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public AccountDto withdraw(
            @Valid @RequestBody CashOperationDto operationDto,
            JwtAuthenticationToken authentication) {
        String login = JwtUtils.getLogin(authentication.getToken());
        return cashService.withdraw(login, operationDto);
    }
}
