package ru.practicum.cash.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.cash.client.NotificationsClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = {NotificationsClient.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(NotificationsContractTestConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.CLASSPATH,
        ids = "ru.practicum:notifications-service:0.0.1-SNAPSHOT:stubs:18082"
)
class NotificationsClientContractTest {

    @Autowired
    private NotificationsClient notificationsClient;

    @Test
    void shouldSendDepositNotificationAccordingToContract() {
        assertDoesNotThrow(() ->
                notificationsClient.notifyDeposit("user", new BigDecimal("100.00")));
    }
}
