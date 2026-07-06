package ru.practicum.transfer.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.kafka.NotificationProducer;
import ru.practicum.transfer.model.NotificationType;
import ru.practicum.transfer.model.TransferException;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountsClient accountsClient;
    private final NotificationProducer notificationProducer;
    private final MeterRegistry meterRegistry;

    public TransferService(AccountsClient accountsClient,
                           NotificationProducer notificationProducer,
                           MeterRegistry meterRegistry) {
        this.accountsClient = accountsClient;
        this.notificationProducer = notificationProducer;
        this.meterRegistry = meterRegistry;
    }

    public TransferResponseDto transfer(String fromLogin, TransferDto transferDto) {
        String toLogin = transferDto.getToLogin();

        log.info("Запрос перевода: from={}, to={}, amount={}",
                fromLogin, toLogin, transferDto.getAmount());

        if (fromLogin.equals(toLogin)) {
            throw new TransferException("Нельзя переводить деньги самому себе");
        }

        try {
            TransferResponseDto result = accountsClient.transfer(
                    fromLogin, toLogin, transferDto.getAmount());

            log.info("Перевод выполнен: from={}, to={}", fromLogin, toLogin);

            notificationProducer.send(fromLogin,
                    "Перевод " + transferDto.getAmount() + " пользователю " + toLogin,
                    NotificationType.TRANSFER_OUT);
            notificationProducer.send(toLogin,
                    "Поступил перевод " + transferDto.getAmount() + " от " + fromLogin,
                    NotificationType.TRANSFER_IN);

            meterRegistry.counter("bank_transfer_total",
                    "from_login", fromLogin, "to_login", toLogin).increment();

            return result;
        } catch (Exception ex) {
            meterRegistry.counter("bank_transfer_failed_total",
                    "from_login", fromLogin, "to_login", toLogin).increment();
            throw ex;
        }
    }
}
