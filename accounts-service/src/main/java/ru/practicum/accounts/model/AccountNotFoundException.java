package ru.practicum.accounts.model;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String login) {
        super("Аккаунт с логином '" + login + "' не найден");
    }
}
