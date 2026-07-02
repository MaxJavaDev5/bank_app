package ru.practicum.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.InsufficientFundsException;
import ru.practicum.accounts.model.TransferException;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.service.AccountTransactionService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountTransactionService transactionService;

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

        AccountDto result = transactionService.updateBalance("user", updateDto);

        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        assertNotNull(result);
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

        AccountDto result = transactionService.updateBalance("user", updateDto);

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        assertNotNull(result);
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
                () -> transactionService.updateBalance("user", updateDto));

        verify(accountRepository, never()).save(any());
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

        TransferResponseDto result = transactionService.transfer(
                "user", "user2", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), sender.getBalance());
        assertEquals(new BigDecimal("800.00"), receiver.getBalance());
        assertEquals(new BigDecimal("700.00"), result.getSenderBalance());
    }

    @Test
    void shouldThrowErrorWhenTransferToSelf() {
        assertThrows(TransferException.class,
                () -> transactionService.transfer("user", "user", new BigDecimal("100.00")));

        verify(accountRepository, never()).findByLogin(any());
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
                () -> transactionService.transfer("user", "user2", new BigDecimal("100.00")));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldThrowErrorWhenReceiverNotFoundOnTransfer() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transactionService.transfer("user", "unknown", new BigDecimal("100.00")));
    }

    @Test
    void shouldThrowWhenTransferFailsOnSave() {
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
                () -> transactionService.transfer("user", "user2", new BigDecimal("300.00")));
    }
}
