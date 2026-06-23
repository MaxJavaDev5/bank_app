package ru.practicum.cash.model;

public class AccountsServiceUnavailableException extends RuntimeException {

    public AccountsServiceUnavailableException(Throwable cause) {
        super("Сервис счетов временно недоступен", cause);
    }
}
