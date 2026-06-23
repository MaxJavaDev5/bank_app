package ru.practicum.transfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.transfer.client.AccountsClient;
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

    @InjectMocks
    private TransferService transferService;

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
        assertEquals(new BigDecimal("700.00"), result.getNewBalanceOfSender());

        verify(accountsClient, times(1)).transfer("user", "user2", new BigDecimal("300.00"));
    }

    @Test
    void shouldThrowErrorWhenTransferToSelf() {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user");
        transferDto.setAmount(new BigDecimal("100.00"));

        assertThrows(TransferException.class,
                () -> transferService.transfer("user", transferDto));

        verifyNoInteractions(accountsClient);
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
    }
}
