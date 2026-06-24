package ru.practicum.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.notifications.controller.GlobalExceptionHandler;
import ru.practicum.notifications.controller.NotificationController;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.service.NotificationService;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs"
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    void shouldReturn403WhenUserTokenOnCreateNotification() throws Exception {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(1L);
        request.setLogin("user");
        request.setMessage("Пополнение на 100 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        mockMvc.perform(post("/notifications")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenServiceTokenOnGetMyNotifications() throws Exception {
        mockMvc.perform(get("/notifications/me")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))
                                .jwt(builder -> builder
                                        .subject("accounts-service")
                                        .claim("realm_access", Map.of("roles", List.of("SERVICE"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(1L);
        request.setLogin("user");
        request.setMessage("Пополнение на 100 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
