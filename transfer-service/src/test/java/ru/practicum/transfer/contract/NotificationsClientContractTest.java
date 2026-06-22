package ru.practicum.transfer.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.transfer.client.NotificationsClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = {NotificationsClient.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({NotificationsContractTestConfig.class, StubRunnerLocalRepositoryConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "ru.practicum:notifications-service:0.0.1-SNAPSHOT:stubs:18084"
)
class NotificationsClientContractTest {

    @Autowired
    private NotificationsClient notificationsClient;

    @Test
    void shouldSendTransferOutNotificationAccordingToContract() {
        assertDoesNotThrow(() ->
                notificationsClient.notifySender(
                        "user", "user2", new BigDecimal("100.00")));
    }
}
