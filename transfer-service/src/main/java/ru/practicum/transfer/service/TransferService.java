package ru.practicum.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.client.NotificationsClient;
import ru.practicum.transfer.dto.AccountDto;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.model.TransferException;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    public TransferService(AccountsClient accountsClient, NotificationsClient notificationsClient) {
        this.accountsClient = accountsClient;
        this.notificationsClient = notificationsClient;
    }

    public TransferResponseDto transfer(TransferDto transferDto) {
        String fromLogin = transferDto.getFromLogin();
        String toLogin = transferDto.getToLogin();

        log.info("Запрос перевода: from={}, to={}, amount={}",
                fromLogin, toLogin, transferDto.getAmount());


        // нельзя переводить самому себе
        if (fromLogin.equals(toLogin)) {
            throw new TransferException("Нельзя переводить деньги самому себе");
        }

        AccountDto senderAccount = accountsClient.withdraw(fromLogin, transferDto.getAmount());
        accountsClient.deposit(toLogin, transferDto.getAmount());

        notificationsClient.notifySender(fromLogin, toLogin, transferDto.getAmount());
        notificationsClient.notifyReceiver(toLogin, fromLogin, transferDto.getAmount());

        log.info("Перевод выполнен: from={}, to={}", fromLogin, toLogin);

        return new TransferResponseDto(
                fromLogin,
                toLogin,
                transferDto.getAmount(),
                senderAccount.getBalance()
        );
    }
}
