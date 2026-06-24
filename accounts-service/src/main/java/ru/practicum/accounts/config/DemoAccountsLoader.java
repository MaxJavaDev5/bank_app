package ru.practicum.accounts.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.practicum.accounts.model.Account;
import ru.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DemoAccountsLoader implements ApplicationRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(ApplicationArguments args) {
        // если в базе уже что-то есть - не трогаем
        if (accountRepository.count() > 0) {
            return;
        }

        accountRepository.save(createAccount("user", "Иван", "Иванов",
                LocalDate.of(1990, 5, 15), new BigDecimal("10000.00")));
        accountRepository.save(createAccount("user2", "Мария", "Петрова",
                LocalDate.of(1995, 8, 22), new BigDecimal("5000.00")));
        accountRepository.save(createAccount("user3", "Алексей", "Сидоров",
                LocalDate.of(1988, 3, 10), new BigDecimal("7500.00")));
    }

    private static Account createAccount(String login, String firstName, String lastName,
                                         LocalDate birthDate, BigDecimal balance) {
        Account account = new Account();
        account.setLogin(login);
        account.setFirstName(firstName);
        account.setLastName(lastName);
        account.setBirthDate(birthDate);
        account.setBalance(balance);
        return account;
    }
}
