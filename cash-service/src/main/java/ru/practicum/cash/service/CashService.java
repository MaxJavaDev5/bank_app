package ru.practicum.cash.service;

import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;

    public CashService(AccountsClient accountsClient,
                       NotificationProducer notificationProducer,
                       MeterRegistry meterRegistry) {
        this.accountsClient = accountsClient;
        this.notificationProducer = notificationProducer;
        this.meterRegistry = meterRegistry;
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
        try {
            AccountDto account = accountsClient.withdraw(login, operationDto.getAmount());
            notificationProducer.send(login,
                    "Снятие на сумму " + operationDto.getAmount(),
                    NotificationType.WITHDRAW);
            return account;
        } catch (Exception ex) {
            meterRegistry.counter("bank_withdraw_failed_total", "login", login).increment();
            throw ex;
        }
    }
}
