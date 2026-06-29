package ru.practicum.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.accounts.dto.AccountDto;
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
import ru.practicum.accounts.service.AccountService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private OutboxRepository outboxRepository;

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
    void shouldDepositMoneyToAccount() {
        Account account = new Account();
        account.setLogin("user");
        account.setBalance(new BigDecimal("1000.00"));

        AccountDto expectedDto = new AccountDto();
        expectedDto.setBalance(new BigDecimal("1500.00"));

        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountDto(account)).thenReturn(expectedDto);

        AccountDto result = accountService.updateBalance("user", updateDto);

        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        assertNotNull(result);
        verify(outboxRepository, times(1)).save(argThat(event ->
                event.getLogin().equals("user")
                        && event.getEventType() == NotificationType.DEPOSIT
        ));
    }

    @Test
    void shouldWithdrawMoneyFromAccount() {
        Account account = new Account();
        account.setLogin("user");
        account.setBalance(new BigDecimal("1000.00"));

        AccountDto expectedDto = new AccountDto();
        expectedDto.setBalance(new BigDecimal("700.00"));

        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("300.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.WITHDRAW);

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountDto(account)).thenReturn(expectedDto);

        AccountDto result = accountService.updateBalance("user", updateDto);

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        assertNotNull(result);
        verify(outboxRepository, times(1)).save(argThat(event ->
                event.getLogin().equals("user")
                        && event.getEventType() == NotificationType.WITHDRAW
        ));
    }

    @Test
    void shouldThrowErrorWhenNotEnoughMoneyForWithdraw() {
        Account account = new Account();
        account.setLogin("user");
        account.setBalance(new BigDecimal("100.00"));

        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.WITHDRAW);

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.updateBalance("user", updateDto));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldSaveOutboxEventWhenAccountUpdated() {
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

        verify(outboxRepository, times(1)).save(argThat(event ->
                event.getLogin().equals("user")
                        && event.getEventType() == NotificationType.PROFILE_UPDATE
                        && event.getStatus() == OutboxStatus.PENDING
        ));
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        Account sender = new Account();
        sender.setLogin("user");
        sender.setBalance(new BigDecimal("1000.00"));

        Account receiver = new Account();
        receiver.setLogin("user2");
        receiver.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(sender));
        when(accountRepository.findByLogin("user2")).thenReturn(Optional.of(receiver));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponseDto result = accountService.transfer(
                "user", "user2", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), sender.getBalance());
        assertEquals(new BigDecimal("800.00"), receiver.getBalance());
        assertEquals(new BigDecimal("700.00"), result.getSenderBalance());
        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowErrorWhenTransferToSelf() {
        assertThrows(TransferException.class,
                () -> accountService.transfer("user", "user", new BigDecimal("100.00")));

        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldThrowErrorWhenNotEnoughMoneyOnTransfer() {
        Account sender = new Account();
        sender.setLogin("user");
        sender.setBalance(new BigDecimal("50.00"));

        Account receiver = new Account();
        receiver.setLogin("user2");

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(sender));
        when(accountRepository.findByLogin("user2")).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.transfer("user", "user2", new BigDecimal("100.00")));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldThrowErrorWhenReceiverNotFoundOnTransfer() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.transfer("user", "unknown", new BigDecimal("100.00")));
    }

    @Test
    void shouldNotSaveOutboxWhenTransferFailsOnSave() {
        Account sender = new Account();
        sender.setLogin("user");
        sender.setBalance(new BigDecimal("1000.00"));

        Account receiver = new Account();
        receiver.setLogin("user2");
        receiver.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(sender));
        when(accountRepository.findByLogin("user2")).thenReturn(Optional.of(receiver));
        when(accountRepository.save(sender)).thenReturn(sender);
        when(accountRepository.save(receiver)).thenThrow(new RuntimeException("save failed"));

        assertThrows(RuntimeException.class,
                () -> accountService.transfer("user", "user2", new BigDecimal("300.00")));

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void shouldRetryUpdateBalanceWhenOptimisticLockConflict() {
        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        AccountDto expectedDto = new AccountDto();
        expectedDto.setBalance(new BigDecimal("1500.00"));

        when(accountRepository.findByLogin("user")).thenAnswer(invocation -> {
            Account account = new Account();
            account.setLogin("user");
            account.setBalance(new BigDecimal("1000.00"));
            return Optional.of(account);
        });
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.toAccountDto(any(Account.class))).thenReturn(expectedDto);

        AccountDto result = accountService.updateBalance("user", updateDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void shouldThrowWhenOptimisticLockConflictExhaustedOnUpdateBalance() {
        Account account = new Account();
        account.setLogin("user");
        account.setBalance(new BigDecimal("1000.00"));

        UpdateBalanceDto updateDto = new UpdateBalanceDto();
        updateDto.setAmount(new BigDecimal("500.00"));
        updateDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        when(accountRepository.findByLogin("user")).thenReturn(Optional.of(account));
        when(accountRepository.save(account))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

        assertThrows(OptimisticLockingFailureException.class,
                () -> accountService.updateBalance("user", updateDto));

        verify(accountRepository, times(3)).save(account);
    }
}
