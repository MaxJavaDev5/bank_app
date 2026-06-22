package ru.practicum.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.client.NotificationsClient;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;

@Service
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);

    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    public CashService(AccountsClient accountsClient, NotificationsClient notificationsClient) {
        this.accountsClient = accountsClient;
        this.notificationsClient = notificationsClient;
    }

    // пополняем через accounts, потом отправляем уведомление
    public AccountDto deposit(CashOperationDto operationDto) {
        log.info("Пополнение: login={}, amount={}", operationDto.getLogin(), operationDto.getAmount());
        AccountDto updatedAccount = accountsClient.deposit(
                operationDto.getLogin(),
                operationDto.getAmount()
        );
        notificationsClient.notifyDeposit(
                operationDto.getLogin(),
                operationDto.getAmount()
        );

        return updatedAccount;
    }

    public AccountDto withdraw(CashOperationDto operationDto) {
        log.info("Снятие: login={}, amount={}", operationDto.getLogin(), operationDto.getAmount());

        AccountDto updatedAccount = accountsClient.withdraw(
                operationDto.getLogin(),
                operationDto.getAmount()
        );

        notificationsClient.notifyWithdraw(
                operationDto.getLogin(),
                operationDto.getAmount()
        );

        return updatedAccount;
    }
}
