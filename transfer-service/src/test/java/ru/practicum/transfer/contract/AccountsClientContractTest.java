package ru.practicum.transfer.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.model.RemoteException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {AccountsClient.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({AccountsContractTestConfig.class, StubRunnerLocalRepositoryConfig.class})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "ru.practicum:accounts-service:0.0.1-SNAPSHOT:stubs:18083"
)
class AccountsClientContractTest {

    @Autowired
    private AccountsClient accountsClient;

    @Test
    void shouldTransferMoneyAccordingToContract() {
        TransferResponseDto result = accountsClient.transfer(
                "user", "user2", new BigDecimal("300.00"));

        assertNotNull(result);
        assertEquals("user", result.getFromLogin());
        assertEquals("user2", result.getToLogin());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
        assertEquals(new BigDecimal("700.00"), result.getNewBalanceOfSender());
    }

    @Test
    void shouldFailTransferWhenInsufficientFunds() {
        assertThrows(RemoteException.class,
                () -> accountsClient.transfer("poor-user", "user2", new BigDecimal("300.00")));
    }

    @Test
    void shouldFailTransferWhenAccountNotFound() {
        assertThrows(RemoteException.class,
                () -> accountsClient.transfer("user", "unknown", new BigDecimal("100.00")));
    }
}
