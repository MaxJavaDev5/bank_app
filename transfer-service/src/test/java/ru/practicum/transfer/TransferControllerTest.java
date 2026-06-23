package ru.practicum.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.transfer.SecurityConfig;
import ru.practicum.transfer.controller.GlobalExceptionHandler;
import ru.practicum.transfer.controller.TransferController;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.model.AccountsServiceUnavailableException;
import ru.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs"
})
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @Test
    void shouldTransferMoneyWithStatus200() throws Exception {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("300.00"));

        TransferResponseDto resultDto = new TransferResponseDto(
                "user", "user2", new BigDecimal("300.00"), new BigDecimal("700.00"));

        when(transferService.transfer(eq("user"), any(TransferDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/transfer")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("transfer.write"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "TRANSFER_WRITE")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromLogin").value("user"))
                .andExpect(jsonPath("$.toLogin").value("user2"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.newBalanceOfSender").value(700.00));
    }

    @Test
    void shouldUseLoginFromJwtPreferredUsername() throws Exception {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("100.00"));

        TransferResponseDto resultDto = new TransferResponseDto(
                "user", "user2", new BigDecimal("100.00"), new BigDecimal("900.00"));

        when(transferService.transfer(eq("user"), any(TransferDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/transfer")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("transfer.write"))
                                .jwt(builder -> builder
                                        .subject("someone-else")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "TRANSFER_WRITE")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isOk());

        verify(transferService).transfer(eq("user"), any(TransferDto.class));
    }

    @Test
    void shouldReturn503WhenAccountsServiceUnavailable() throws Exception {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("100.00"));

        when(transferService.transfer(eq("user"), any(TransferDto.class)))
                .thenThrow(new AccountsServiceUnavailableException(new RuntimeException("timeout")));

        mockMvc.perform(post("/transfer")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("transfer.write"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "TRANSFER_WRITE")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Сервис счетов временно недоступен"));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        TransferDto transferDto = new TransferDto();
        transferDto.setToLogin("user2");
        transferDto.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isUnauthorized());
    }
}
