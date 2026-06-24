package ru.practicum.cash.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.model.RemoteException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {AccountsClient.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({AccountsContractTestConfig.class, StubRunnerLocalRepositoryConfig.class})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "ru.practicum:accounts-service:0.0.1-SNAPSHOT:stubs:18081"
)
class AccountsClientContractTest {

    @Autowired
    private AccountsClient accountsClient;

    @Test
    void shouldDepositMoneyAccordingToContract() {
        AccountDto result = accountsClient.deposit("user", new BigDecimal("100.00"));

        assertNotNull(result);
        assertEquals("user", result.getLogin());
        assertEquals(new BigDecimal("1100.00"), result.getBalance());
    }

    @Test
    void shouldWithdrawMoneyAccordingToContract() {
        AccountDto result = accountsClient.withdraw("user", new BigDecimal("100.00"));

        assertNotNull(result);
        assertEquals("user", result.getLogin());
        assertEquals(new BigDecimal("900.00"), result.getBalance());
    }

    @Test
    void shouldFailWithdrawWhenInsufficientFunds() {
        assertThrows(RemoteException.class,
                () -> accountsClient.withdraw("poor-user", new BigDecimal("500.00")));
    }

    @Test
    void shouldFailWithdrawWhenAccountNotFound() {
        assertThrows(RemoteException.class,
                () -> accountsClient.withdraw("unknown", new BigDecimal("100.00")));
    }
}
