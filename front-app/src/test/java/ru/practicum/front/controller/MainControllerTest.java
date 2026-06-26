package ru.practicum.front.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.practicum.front.client.BankClient;
import ru.practicum.front.dto.AccountDto;
import ru.practicum.front.dto.AccountShortDto;
import ru.practicum.front.dto.CashForm;
import ru.practicum.front.dto.TransferForm;
import ru.practicum.front.dto.UpdateAccountForm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private BankClient bankClient;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

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

    @Test
    void shouldReturnValidationErrorsWhenCashFormInvalid() {
        CashForm form = new CashForm();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = mainController.editCash(form, bindingResult, redirectAttributes, null);

        assertEquals("redirect:/account", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("errors"), anyList());
        verifyNoInteractions(bankClient);
    }

    @Test
    void shouldReturnValidationErrorsWhenTransferFormInvalid() {
        TransferForm form = new TransferForm();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = mainController.transfer(form, bindingResult, redirectAttributes, null);

        assertEquals("redirect:/account", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("errors"), anyList());
        verifyNoInteractions(bankClient);
    }

    @Test
    void shouldReturnValidationErrorsWhenUpdateAccountFormInvalid() {
        UpdateAccountForm form = new UpdateAccountForm();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = mainController.editAccount(form, bindingResult, redirectAttributes, null);

        assertEquals("redirect:/account", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("errors"), anyList());
        verifyNoInteractions(bankClient);
    }
}
