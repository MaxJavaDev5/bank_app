package ru.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.accounts.model.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByLogin(String login);

    boolean existsByLogin(String login);
}
