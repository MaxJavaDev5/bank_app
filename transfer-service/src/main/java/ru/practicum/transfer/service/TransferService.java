package ru.practicum.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.model.TransferException;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountsClient accountsClient;

    public TransferService(AccountsClient accountsClient) {
        this.accountsClient = accountsClient;
    }

    public TransferResponseDto transfer(String fromLogin, TransferDto transferDto) {
        String toLogin = transferDto.getToLogin();

        log.info("Запрос перевода: from={}, to={}, amount={}",
                fromLogin, toLogin, transferDto.getAmount());


        // нельзя переводить самому себе
        if (fromLogin.equals(toLogin)) {
            throw new TransferException("Нельзя переводить деньги самому себе");
        }

        TransferResponseDto result = accountsClient.transfer(
                fromLogin, toLogin, transferDto.getAmount());

        log.info("Перевод выполнен: from={}, to={}", fromLogin, toLogin);

        return result;
    }
}
