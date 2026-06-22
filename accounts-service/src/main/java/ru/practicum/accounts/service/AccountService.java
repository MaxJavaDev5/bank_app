package ru.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.AccountShortDto;
import ru.practicum.accounts.dto.UpdateAccountDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.InsufficientFundsException;
import ru.practicum.accounts.model.NotificationType;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.OutboxRepository;

import java.math.BigDecimal;
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
        saveProfileUpdateEvent(login);
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

        if (updateBalanceDto.getOperationType() == UpdateBalanceDto.OperationType.DEPOSIT) {
            BigDecimal newBalance = account.getBalance().add(updateBalanceDto.getAmount());
            account.setBalance(newBalance);
        } else {  // WITHDRAW
            if (account.getBalance().compareTo(updateBalanceDto.getAmount()) < 0) {
                throw new InsufficientFundsException(
                        login,
                        account.getBalance(),
                        updateBalanceDto.getAmount()
                );
            }
            BigDecimal newBalance = account.getBalance().subtract(updateBalanceDto.getAmount());
            account.setBalance(newBalance);
        }

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toAccountDto(savedAccount);
    }

    private void saveProfileUpdateEvent(String login) {
        OutboxEvent event = new OutboxEvent();
        event.setLogin(login);
        event.setMessage("Ваш профиль успешно обновлён");
        event.setEventType(NotificationType.PROFILE_UPDATE);
        event.setProcessed(false);
        outboxRepository.save(event);
    }

    private Account findAccountOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }
}
