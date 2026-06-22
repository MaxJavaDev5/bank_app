package ru.practicum.cash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.client.NotificationsClient;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;
import ru.practicum.cash.model.RemoteException;
import ru.practicum.cash.service.CashService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private CashService cashService;

    @Test
    void shouldDepositMoneyAndSendNotification() {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setLogin("user");
        operationDto.setAmount(new BigDecimal("500.00"));

        AccountDto expectedAccount = new AccountDto();
        expectedAccount.setLogin("user");
        expectedAccount.setBalance(new BigDecimal("1500.00"));

        when(accountsClient.deposit("user", new BigDecimal("500.00")))
                .thenReturn(expectedAccount);

        AccountDto result = cashService.deposit(operationDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());

        verify(accountsClient, times(1))
                .deposit("user", new BigDecimal("500.00"));
        verify(notificationsClient, times(1))
                .notifyDeposit("user", new BigDecimal("500.00"));
    }

    @Test
    void shouldWithdrawMoneyAndSendNotification() {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setLogin("user");
        operationDto.setAmount(new BigDecimal("200.00"));

        AccountDto expectedAccount = new AccountDto();
        expectedAccount.setLogin("user");
        expectedAccount.setBalance(new BigDecimal("800.00"));

        when(accountsClient.withdraw("user", new BigDecimal("200.00")))
                .thenReturn(expectedAccount);

        AccountDto result = cashService.withdraw(operationDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("800.00"), result.getBalance());

        verify(notificationsClient, times(1))
                .notifyWithdraw("user", new BigDecimal("200.00"));
    }

    @Test
    void shouldPropagateErrorFromAccountsService() {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setLogin("user");
        operationDto.setAmount(new BigDecimal("99999.00"));

        when(accountsClient.withdraw("user", new BigDecimal("99999.00")))
                .thenThrow(new RemoteException("accounts-service", "Недостаточно средств"));

        assertThrows(RemoteException.class,
                () -> cashService.withdraw(operationDto));

        verify(notificationsClient, never())
                .notifyWithdraw(any(), any());
    }
}
