package ru.practicum.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;

import java.math.BigDecimal;

@Service
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);

    private final AccountsClient accountsClient;

    public CashService(AccountsClient accountsClient) {
        this.accountsClient = accountsClient;
    }

    // пополняем через accounts
    public AccountDto deposit(String login, CashOperationDto operationDto) {
        log.info("Пополнение: login={}, amount={}", login, operationDto.getAmount());
        return accountsClient.deposit(login, operationDto.getAmount());
    }

    public AccountDto withdraw(String login, CashOperationDto operationDto) {
        log.info("Снятие: login={}, amount={}", login, operationDto.getAmount());
        return accountsClient.withdraw(login, operationDto.getAmount());
    }
}
