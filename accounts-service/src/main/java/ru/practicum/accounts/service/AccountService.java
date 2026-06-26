package ru.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.AccountShortDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateAccountDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.InsufficientFundsException;
import ru.practicum.accounts.model.NotificationType;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.model.OutboxStatus;
import ru.practicum.accounts.model.TransferException;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.OutboxRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxRepository outboxRepository;

    // возращает аккаунт по логину
    @Transactional(readOnly = true)
    public AccountDto getAccountByLogin(String login) {
        Account account = findAccountOrThrow(login);
        return accountMapper.toAccountDto(account);
    }

    @Transactional
    public AccountDto updateAccount(String login, UpdateAccountDto updateDto) {
        Account account = findAccountOrThrow(login);
        accountMapper.updateAccountFromDto(updateDto, account);
        Account savedAccount = accountRepository.save(account);
        saveOutboxEvent(login, "Ваш профиль успешно обновлён", NotificationType.PROFILE_UPDATE);
        return accountMapper.toAccountDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountShortDto> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return accountMapper.toAccountShortDtoList(accounts);
    }

    @Transactional
    public AccountDto updateBalance(String login, UpdateBalanceDto updateBalanceDto) {
        Account account = findAccountOrThrow(login);
        BigDecimal amount = updateBalanceDto.getAmount();

        if (updateBalanceDto.getOperationType() == UpdateBalanceDto.OperationType.DEPOSIT) {
            account.setBalance(account.getBalance().add(amount));
            saveOutboxEvent(login, "Ваш счёт пополнен на " + amount + " рублей", NotificationType.DEPOSIT);
        } else {  // WITHDRAW
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(login, account.getBalance(), amount);
            }
            account.setBalance(account.getBalance().subtract(amount));
            saveOutboxEvent(login, "Со счёта снято " + amount + " рублей", NotificationType.WITHDRAW);
        }

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toAccountDto(savedAccount);
    }

    @Transactional
    public TransferResponseDto transfer(String fromLogin, String toLogin, BigDecimal amount) {
        if (fromLogin.equals(toLogin)) {
            throw new TransferException("Нельзя переводить деньги самому себе");
        }

        Account first;
        Account second;
        if (fromLogin.compareTo(toLogin) < 0) {
            first = findAccountOrThrow(fromLogin);
            second = findAccountOrThrow(toLogin);
        } else {
            first = findAccountOrThrow(toLogin);
            second = findAccountOrThrow(fromLogin);
        }

        Account sender = fromLogin.equals(first.getLogin()) ? first : second;
        Account receiver = fromLogin.equals(first.getLogin()) ? second : first;

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromLogin, sender.getBalance(), amount);
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        saveOutboxEvent(fromLogin, "Вы перевели " + amount + " руб. пользователю " + toLogin,
                NotificationType.TRANSFER_OUT);
        saveOutboxEvent(toLogin, "Вы получили " + amount + " рублей от " + fromLogin,
                NotificationType.TRANSFER_IN);

        return new TransferResponseDto(fromLogin, toLogin, amount, sender.getBalance());
    }

    private void saveOutboxEvent(String login, String message, NotificationType type) {
        OutboxEvent event = new OutboxEvent();
        event.setLogin(login);
        event.setMessage(message);
        event.setEventType(type);
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        outboxRepository.save(event);
    }

    private Account findAccountOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }
}
