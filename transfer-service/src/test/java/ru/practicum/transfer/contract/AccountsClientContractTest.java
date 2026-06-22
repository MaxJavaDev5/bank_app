package ru.practicum.transfer.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.dto.AccountDto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {AccountsClient.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({AccountsContractTestConfig.class, StubRunnerLocalRepositoryConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "ru.practicum:accounts-service:0.0.1-SNAPSHOT:stubs:18083"
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
}
