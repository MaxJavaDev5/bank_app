package ru.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.InsufficientFundsException;
import ru.practicum.accounts.model.TransferException;
import ru.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountTransactionService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountDto updateBalance(String login, UpdateBalanceDto updateBalanceDto) {
        Account account = findAccountOrThrow(login);
        BigDecimal amount = updateBalanceDto.getAmount();

        if (updateBalanceDto.getOperationType() == UpdateBalanceDto.OperationType.DEPOSIT) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(login, account.getBalance(), amount);
            }
            account.setBalance(account.getBalance().subtract(amount));
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

        return new TransferResponseDto(fromLogin, toLogin, amount, sender.getBalance());
    }

    private Account findAccountOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }
}
