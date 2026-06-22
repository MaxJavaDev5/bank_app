package ru.practicum.cash;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.cash.SecurityConfig;
import ru.practicum.cash.controller.CashController;
import ru.practicum.cash.controller.GlobalExceptionHandler;
import ru.practicum.cash.dto.AccountDto;
import ru.practicum.cash.dto.CashOperationDto;
import ru.practicum.cash.service.CashService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CashController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/bank-realm/protocol/openid-connect/certs"
})
class CashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CashService cashService;

    @Test
    void shouldDepositMoneyWithStatus200() throws Exception {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setLogin("user");
        operationDto.setAmount(new BigDecimal("500.00"));

        AccountDto accountDto = new AccountDto();
        accountDto.setLogin("user");
        accountDto.setBalance(new BigDecimal("1500.00"));

        when(cashService.deposit(any(CashOperationDto.class))).thenReturn(accountDto);

        mockMvc.perform(post("/cash/deposit")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("user"))
                .andExpect(jsonPath("$.balance").value(1500.00));
    }

    @Test
    void shouldWithdrawMoneyWithStatus200() throws Exception {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setLogin("user");
        operationDto.setAmount(new BigDecimal("200.00"));

        AccountDto accountDto = new AccountDto();
        accountDto.setLogin("user");
        accountDto.setBalance(new BigDecimal("800.00"));

        when(cashService.withdraw(any(CashOperationDto.class))).thenReturn(accountDto);

        mockMvc.perform(post("/cash/withdraw")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder
                                        .subject("user")
                                        .claim("preferred_username", "user")
                                        .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("user"))
                .andExpect(jsonPath("$.balance").value(800.00));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        CashOperationDto operationDto = new CashOperationDto();
        operationDto.setLogin("user");
        operationDto.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/cash/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operationDto)))
                .andExpect(status().isUnauthorized());
    }
}
