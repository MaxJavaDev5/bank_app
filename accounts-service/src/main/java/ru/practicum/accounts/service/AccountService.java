package ru.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.AccountShortDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateAccountDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.kafka.NotificationEvent;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.NotificationType;
import ru.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountTransactionService transactionService;

    @Value("${accounts.retry.max-attempts:3}")
    private int maxAttempts;

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
        eventPublisher.publishEvent(new NotificationEvent(
                login, "Ваш профиль успешно обновлён", NotificationType.PROFILE_UPDATE));
        return accountMapper.toAccountDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountShortDto> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return accountMapper.toAccountShortDtoList(accounts);
    }

    public AccountDto updateBalance(String login, UpdateBalanceDto updateBalanceDto) {
        return runWithRetry(() -> transactionService.updateBalance(login, updateBalanceDto));
    }

    public TransferResponseDto transfer(String fromLogin, String toLogin, BigDecimal amount) {
        return runWithRetry(() -> transactionService.transfer(fromLogin, toLogin, amount));
    }

    private Account findAccountOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    private <T> T runWithRetry(Supplier<T> action) {
        int attempt = 1;
        while (true) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException ex) {
                if (attempt >= maxAttempts) {
                    throw ex;
                }
                log.warn("Конфликт версий, повтор {} из {}", attempt, maxAttempts - 1);
                attempt++;
            }
        }
    }
}
