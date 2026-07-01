package ru.practicum.transfer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.kafka.NotificationProducer;
import ru.practicum.transfer.model.NotificationType;
import ru.practicum.transfer.model.RemoteException;
import ru.practicum.transfer.model.TransferException;
import ru.practicum.transfer.service.TransferService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUpMeterRegistry() {
        lenient().when(meterRegistry.counter(
                anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(counter);
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("300.00"));

        TransferResponseDto accountsResponse = new TransferResponseDto(
                "user", "user2", new BigDecimal("300.00"), new BigDecimal("700.00"));

        when(accountsClient.transfer("user", "user2", new BigDecimal("300.00")))
                .thenReturn(accountsResponse);

        TransferResponseDto result = transferService.transfer("user", transferDto);

        assertNotNull(result);
        assertEquals("user", result.getFromLogin());
        assertEquals("user2", result.getToLogin());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
        assertEquals(new BigDecimal("700.00"), result.getSenderBalance());

        verify(accountsClient, times(1)).transfer("user", "user2", new BigDecimal("300.00"));
        verify(notificationProducer, times(1))
                .send(eq("user"), anyString(), eq(NotificationType.TRANSFER_OUT));
        verify(notificationProducer, times(1))
                .send(eq("user2"), anyString(), eq(NotificationType.TRANSFER_IN));
    }

    @Test
    void shouldThrowErrorWhenTransferToSelf() {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user");
        transferDto.setAmount(new BigDecimal("100.00"));

        assertThrows(TransferException.class,
                () -> transferService.transfer("user", transferDto));

        verifyNoInteractions(accountsClient);
        verifyNoInteractions(notificationProducer);
    }

    @Test
    void shouldPropagateErrorFromAccountsService() {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("99999.00"));

        when(accountsClient.transfer("user", "user2", new BigDecimal("99999.00")))
                .thenThrow(new RemoteException("accounts-service", "Недостаточно средств"));

        assertThrows(RemoteException.class,
                () -> transferService.transfer("user", transferDto));

        verify(accountsClient, times(1)).transfer("user", "user2", new BigDecimal("99999.00"));
        verify(notificationProducer, never()).send(anyString(), anyString(), any());
    }
}
