package ru.practicum.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.accounts.config.JwtTestConfig;
import ru.practicum.accounts.SecurityConfig;
import ru.practicum.accounts.controller.AccountController;
import ru.practicum.accounts.controller.GlobalExceptionHandler;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.TransferRequestDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.service.AccountService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtTestConfig.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="
})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    @DisplayName("GET /accounts/me — должен вернуть 200 и данные аккаунта")
    void shouldReturnMyAccountWithStatus200() throws Exception {
        
        AccountDto accountDto = new AccountDto();
        accountDto.setLogin("user");
        accountDto.setFirstName("Иван");
        accountDto.setBalance(new BigDecimal("1000.00"));

        when(accountService.getAccountByLogin("user")).thenReturn(accountDto);

        mockMvc.perform(get("/accounts/me")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("user"))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("GET /accounts/me — должен вернуть 404 если аккаунт не найден")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        when(accountService.getAccountByLogin("unknown"))
                .thenThrow(new AccountNotFoundException("unknown"));

        mockMvc.perform(get("/accounts/me")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("unknown")
                                        .claim("preferred_username", "unknown")
                                        .claim("realm_access", Map.of("roles", List.of("USER"))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /accounts/me — должен вернуть 403 для service-токена")
    void shouldReturn403WhenServiceTokenOnGetMe() throws Exception {
        mockMvc.perform(get("/accounts/me")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))
                                .jwt(builder -> builder
                                        .subject("accounts-service")
                                        .claim("realm_access", Map.of("roles", List.of("SERVICE"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /accounts/{login}/balance — должен вернуть 403 для user-токена")
    void shouldReturn403WhenUserTokenOnUpdateBalance() throws Exception {
        UpdateBalanceDto updateBalanceDto = new UpdateBalanceDto();
        updateBalanceDto.setAmount(new BigDecimal("100.00"));
        updateBalanceDto.setOperationType(UpdateBalanceDto.OperationType.DEPOSIT);

        mockMvc.perform(put("/accounts/user/balance")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBalanceDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/transfer — должен вернуть 403 для user-токена")
    void shouldReturn403WhenUserTokenOnTransfer() throws Exception {
        TransferRequestDto transferRequestDto = new TransferRequestDto();
        transferRequestDto.setFromLogin("user");
        transferRequestDto.setToLogin("user2");
        transferRequestDto.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/accounts/transfer")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /accounts/me — должен вернуть 401 без токена")
    void shouldReturn401WhenNoToken() throws Exception {
        
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }
}
