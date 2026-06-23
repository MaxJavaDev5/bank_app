package ru.practicum.notifications.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.notifications.SecurityConfig;
import ru.practicum.notifications.controller.GlobalExceptionHandler;
import ru.practicum.notifications.controller.NotificationController;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.service.NotificationService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ContractJwtConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="
})
public abstract class ContractBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setupContractTest() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        when(notificationService.createNotification(any(NotificationRequestDto.class)))
                .thenAnswer(invocation -> {
                    NotificationRequestDto request = invocation.getArgument(0);
                    NotificationDto response = new NotificationDto();
                    response.setId(1L);
                    response.setLogin(request.getLogin());
                    response.setMessage(request.getMessage());
                    response.setType(request.getType());
                    response.setEventId(request.getEventId());
                    response.setCreatedAt(LocalDateTime.parse("2024-06-01T12:00:00"));
                    return new NotificationService.NotificationCreationResult(response, true);
                });
    }
}
