package ru.practicum.accounts.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import ru.practicum.accounts.controller.AccountController;
import ru.practicum.accounts.controller.GlobalExceptionHandler;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.TransferRequestDto;
import ru.practicum.accounts.dto.TransferResponseDto;
import ru.practicum.accounts.dto.UpdateBalanceDto;
import ru.practicum.accounts.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public abstract class ContractBaseTest {

    private final AccountService accountService = Mockito.mock(AccountService.class);

    @BeforeEach
    void setupContractTest() {
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

        when(accountService.transfer(eq("user"), eq("user2"), any()))
                .thenReturn(new TransferResponseDto(
                        "user",
                        "user2",
                        new BigDecimal("300.00"),
                        new BigDecimal("700.00")
                ));

        RestAssuredMockMvc.standaloneSetup(
                new AccountController(accountService),
                new GlobalExceptionHandler()
        );
    }
}
