package ru.practicum.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateAccountDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.kafka.NotificationEvent;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.NotificationType;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.service.AccountService;
import ru.practicum.accounts.service.AccountTransactionService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccountTransactionService transactionService;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountService, "maxAttempts", 3);
    }

    @Test
    void shouldReturnAccountByLogin() {
        Account account = new Account();
        account.setLogin("user");
        account.setBalance(new BigDecimal("1000.00"));

        AccountDto expectedDto = new AccountDto();
        expectedDto.setLogin("user");
        expectedDto.setBalance(new BigDecimal("1000.00"));

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(account));
        when(accountMapper.toAccountDto(account)).thenReturn(expectedDto);

        AccountDto result = accountService.getAccountByLogin("user");

        assertNotNull(result);
        assertEquals("user", result.getLogin());
    }

    @Test
    void shouldThrowErrorWhenAccountNotFound() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountByLogin("unknown"));
    }

    @Test
    void shouldDelegateUpdateBalanceToTransactionService() {
        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        AccountDto expectedDto = new AccountDto();
        expectedDto.setBalance(new BigDecimal("1500.00"));

        when(transactionService.updateBalance("user", updateDto)).thenReturn(expectedDto);

        AccountDto result = accountService.updateBalance("user", updateDto);

        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(transactionService).updateBalance("user", updateDto);
    }

    @Test
    void shouldDelegateTransferToTransactionService() {
        TransferResponseDto expectedDto = new TransferResponseDto(
                "user", "user2", new BigDecimal("300.00"), new BigDecimal("700.00"));

        when(transactionService.transfer("user", "user2", new BigDecimal("300.00")))
                .thenReturn(expectedDto);

        TransferResponseDto result = accountService.transfer("user", "user2", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), result.getSenderBalance());
        verify(transactionService).transfer("user", "user2", new BigDecimal("300.00"));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldPublishNotificationEventWhenAccountUpdated() {
        Account account = new Account();
        account.setLogin("user");

        UpdateAccountDto updateDto = new UpdateAccountDto();
        updateDto.setFirstName("Иван");
        updateDto.setLastName("Иванов");
        updateDto.setBirthDate(java.time.LocalDate.of(1990, 1, 1));

        AccountDto expectedDto = new AccountDto();
        expectedDto.setLogin("user");

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountDto(account)).thenReturn(expectedDto);

        accountService.updateAccount("user", updateDto);

        verify(eventPublisher).publishEvent(new NotificationEvent(
                "user", "Ваш профиль успешно обновлён", NotificationType.PROFILE_UPDATE));
    }

    @Test
    void shouldRetryUpdateBalanceWhenOptimisticLockConflict() {
        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        AccountDto expectedDto = new AccountDto();
        expectedDto.setBalance(new BigDecimal("1500.00"));

        when(transactionService.updateBalance("user", updateDto))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenReturn(expectedDto);

        AccountDto result = accountService.updateBalance("user", updateDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(transactionService, times(2)).updateBalance("user", updateDto);
    }

    @Test
    void shouldThrowWhenOptimisticLockConflictExhaustedOnUpdateBalance() {
        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        when(transactionService.updateBalance("user", updateDto))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

        assertThrows(OptimisticLockingFailureException.class,
                () -> accountService.updateBalance("user", updateDto));

        verify(transactionService, times(3)).updateBalance("user", updateDto);
    }
}
