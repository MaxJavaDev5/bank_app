package ru.practicum.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;
import ru.practicum.cash.kafka.NotificationProducer;
import ru.practicum.cash.model.NotificationType;

@Service
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);

    private final AccountsClient accountsClient;
    private final NotificationProducer notificationProducer;

    public CashService(AccountsClient accountsClient, NotificationProducer notificationProducer) {
        this.accountsClient = accountsClient;
        this.notificationProducer = notificationProducer;
    }

    public AccountDto deposit(String login, CashOperationDto operationDto) {
        log.info("Пополнение: login={}, amount={}", login, operationDto.getAmount());
        AccountDto account = accountsClient.deposit(login, operationDto.getAmount());
        notificationProducer.send(login,
                "Пополнение на сумму " + operationDto.getAmount(),
                NotificationType.DEPOSIT);
        return account;
    }

    public AccountDto withdraw(String login, CashOperationDto operationDto) {
        log.info("Снятие: login={}, amount={}", login, operationDto.getAmount());
        AccountDto account = accountsClient.withdraw(login, operationDto.getAmount());
        notificationProducer.send(login,
                "Снятие на сумму " + operationDto.getAmount(),
                NotificationType.WITHDRAW);
        return account;
    }
}
