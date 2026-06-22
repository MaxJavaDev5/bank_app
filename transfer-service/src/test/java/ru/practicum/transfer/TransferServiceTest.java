package ru.practicum.transfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.client.NotificationsClient;
import ru.practicum.transfer.dto.AccountDto;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.model.RemoteException;
import ru.practicum.transfer.model.TransferException;
import ru.practicum.transfer.service.TransferService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private TransferService transferService;

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        TransferDto transferDto = new TransferDto();
        transferDto.setFromLogin("user");
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("300.00"));

        AccountDto senderAfterWithdraw = new AccountDto();
        senderAfterWithdraw.setLogin("user");
        senderAfterWithdraw.setBalance(new BigDecimal("700.00"));

        when(accountsClient.withdraw("user", new BigDecimal("300.00")))
                .thenReturn(senderAfterWithdraw);
        when(accountsClient.deposit("user2", new BigDecimal("300.00")))
                .thenReturn(new AccountDto());

        TransferResponseDto result = transferService.transfer(transferDto);

        assertNotNull(result);
        assertEquals("user", result.getFromLogin());
        assertEquals("user2", result.getToLogin());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
        assertEquals(new BigDecimal("700.00"), result.getNewBalanceOfSender());

        verify(accountsClient, times(1)).withdraw("user", new BigDecimal("300.00"));
        verify(accountsClient, times(1)).deposit("user2", new BigDecimal("300.00"));
        verify(notificationsClient, times(1))
                .notifySender("user", "user2", new BigDecimal("300.00"));
        verify(notificationsClient, times(1))
                .notifyReceiver("user2", "user", new BigDecimal("300.00"));
    }

    @Test
    void shouldThrowErrorWhenTransferToSelf() {
        TransferDto transferDto = new TransferDto();
        transferDto.setFromLogin("user");
        transferDto.setToLogin("user");
        transferDto.setAmount(new BigDecimal("100.00"));

        assertThrows(TransferException.class,
                () -> transferService.transfer(transferDto));

        verifyNoInteractions(accountsClient);
        verifyNoInteractions(notificationsClient);
    }

    @Test
    void shouldNotSendNotificationWhenWithdrawFailed() {
        TransferDto transferDto = new TransferDto();
        transferDto.setFromLogin("user");
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("99999.00"));

        when(accountsClient.withdraw("user", new BigDecimal("99999.00")))
                .thenThrow(new RemoteException("accounts-service", "Недостаточно средств"));

        assertThrows(RemoteException.class,
                () -> transferService.transfer(transferDto));

        verify(accountsClient, never()).deposit(any(), any());
        verifyNoInteractions(notificationsClient);
    }

    @Test
    void shouldNotSendNotificationWhenDepositFailed() {
        TransferDto transferDto = new TransferDto();
        transferDto.setFromLogin("user");
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("100.00"));

        AccountDto senderAccount = new AccountDto();
        senderAccount.setBalance(new BigDecimal("900.00"));

        when(accountsClient.withdraw("user", new BigDecimal("100.00")))
                .thenReturn(senderAccount);
        when(accountsClient.deposit("user2", new BigDecimal("100.00")))
                .thenThrow(new RemoteException("accounts-service", "Получатель не найден"));

        assertThrows(RemoteException.class,
                () -> transferService.transfer(transferDto));

        verifyNoInteractions(notificationsClient);
    }
}
