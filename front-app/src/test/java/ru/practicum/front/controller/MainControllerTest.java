package ru.practicum.front.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import ru.practicum.front.client.BankClient;
import ru.practicum.front.dto.AccountDto;
import ru.practicum.front.dto.AccountShortDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private BankClient bankClient;

    private MainController mainController;

    @BeforeEach
    void setUp() {
        mainController = new MainController(bankClient);
    }

    @Test
    void shouldShowMainPageWithAccountData() {
        AccountDto account = new AccountDto();
        account.setLogin("user");
        account.setFirstName("Иван");
        account.setLastName("Иванов");
        account.setBirthDate(LocalDate.of(2001, 1, 1));
        account.setBalance(new BigDecimal("1000.00"));

        AccountShortDto otherAccount = new AccountShortDto();
        otherAccount.setLogin("user2");
        otherAccount.setFirstName("Пётр");
        otherAccount.setLastName("Петров");

        List<AccountShortDto> allAccounts = new ArrayList<>(List.of(otherAccount));

        when(bankClient.getMyAccount()).thenReturn(account);
        when(bankClient.getAllAccounts()).thenReturn(allAccounts);

        Model model = new ExtendedModelMap();
        String viewName = mainController.getAccount(model, null);

        assertEquals("main", viewName);
        assertEquals("Иванов Иван", model.getAttribute("name"));
        assertEquals(1000, model.getAttribute("sum"));
        assertNotNull(model.getAttribute("accounts"));
    }
}
