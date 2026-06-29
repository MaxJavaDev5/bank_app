package ru.practicum.accounts.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.accounts.SecurityConfig;
import ru.practicum.accounts.controller.AccountController;
import ru.practicum.accounts.controller.GlobalExceptionHandler;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.InsufficientFundsException;
import ru.practicum.accounts.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ContractJwtConfig.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="
})
public abstract class ContractBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @BeforeEach
    void setupContractTest() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        when(accountService.updateBalance(eq("user"), any(UpdateBalanceDto.class)))
                .thenAnswer(invocation -> {
                    UpdateBalanceDto dto = invocation.getArgument(1);
                    AccountDto account = new AccountDto();
                    account.setId(1L);
                    account.setLogin("user");
                    account.setFirstName("Иван");
                    if (dto.getOperationType() == UpdateBalanceDto.OperationType.DEPOSIT) {
                        account.setBalance(new BigDecimal("1100.00"));
                    } else {
                        account.setBalance(new BigDecimal("900.00"));
                    }
                    return account;
                });

        when(accountService.updateBalance(eq("unknown"), any(UpdateBalanceDto.class)))
                .thenThrow(new AccountNotFoundException("unknown"));

        when(accountService.updateBalance(eq("poor-user"), any(UpdateBalanceDto.class)))
                .thenThrow(new InsufficientFundsException(
                        "poor-user", new BigDecimal("100.00"), new BigDecimal("500.00")));

        when(accountService.transfer(eq("user"), eq("user2"), any()))
                .thenReturn(new TransferResponseDto(
                        "user",
                        "user2",
                        new BigDecimal("300.00"),
                        new BigDecimal("700.00")
                ));

        when(accountService.transfer(eq("poor-user"), eq("user2"), any()))
                .thenThrow(new InsufficientFundsException(
                        "poor-user", new BigDecimal("100.00"), new BigDecimal("300.00")));

        when(accountService.transfer(eq("user"), eq("unknown"), any()))
                .thenThrow(new AccountNotFoundException("unknown"));
    }
}
