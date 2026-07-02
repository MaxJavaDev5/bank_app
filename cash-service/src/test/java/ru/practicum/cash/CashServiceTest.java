package ru.practicum.cash;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;
import ru.practicum.cash.kafka.NotificationProducer;
import ru.practicum.cash.model.NotificationType;
import ru.practicum.cash.model.RemoteException;
import ru.practicum.cash.service.CashService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private CashService cashService;

    @BeforeEach
    void setUpMeterRegistry() {
        lenient().when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    }

    @Test
    void shouldDepositMoney() {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setAmount(new BigDecimal("500.00"));

        AccountDto expectedAccount = new AccountDto();
        expectedAccount.setLogin("user");
        expectedAccount.setBalance(new BigDecimal("1500.00"));

        when(accountsClient.deposit("user", new BigDecimal("500.00")))
                .thenReturn(expectedAccount);

        AccountDto result = cashService.deposit("user", operationDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(accountsClient, times(1)).deposit("user", new BigDecimal("500.00"));
        verify(notificationProducer, times(1))
                .send(eq("user"), anyString(), eq(NotificationType.DEPOSIT));
    }

    @Test
    void shouldWithdrawMoney() {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setAmount(new BigDecimal("200.00"));

        AccountDto expectedAccount = new AccountDto();
        expectedAccount.setLogin("user");
        expectedAccount.setBalance(new BigDecimal("800.00"));

        when(accountsClient.withdraw("user", new BigDecimal("200.00")))
                .thenReturn(expectedAccount);

        AccountDto result = cashService.withdraw("user", operationDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("800.00"), result.getBalance());
        verify(notificationProducer, times(1))
                .send(eq("user"), anyString(), eq(NotificationType.WITHDRAW));
    }

    @Test
    void shouldPropagateErrorFromAccountsService() {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setAmount(new BigDecimal("99999.00"));

        when(accountsClient.withdraw("user", new BigDecimal("99999.00")))
                .thenThrow(new RemoteException("accounts-service", "Недостаточно средств"));

        assertThrows(RemoteException.class,
                () -> cashService.withdraw("user", operationDto));

        verify(meterRegistry).counter("bank_withdraw_failed_total", "login", "user");
        verify(counter).increment();
        verify(notificationProducer, never()).send(anyString(), anyString(), any());
    }
}
