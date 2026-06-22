package ru.practicum.notifications.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import ru.practicum.notifications.controller.GlobalExceptionHandler;
import ru.practicum.notifications.controller.NotificationController;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.service.NotificationService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public abstract class ContractBaseTest {

    private final NotificationService notificationService = Mockito.mock(NotificationService.class);

    @BeforeEach
    void setupContractTest() {
        when(notificationService.createNotification(any(NotificationRequestDto.class)))
                .thenAnswer(invocation -> {
                    NotificationRequestDto request = invocation.getArgument(0);
                    NotificationDto response = new NotificationDto();
                    response.setId(1L);
                    response.setLogin(request.getLogin());
                    response.setMessage(request.getMessage());
                    response.setType(request.getType());
                    response.setCreatedAt(LocalDateTime.parse("2024-06-01T12:00:00"));
                    return response;
                });

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RestAssuredMockMvc.standaloneSetup(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new NotificationController(notificationService),
                new GlobalExceptionHandler()
        );
    }
}
