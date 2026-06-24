package ru.practicum.transfer.model;

public class AccountsServiceUnavailableException extends RuntimeException {

    public AccountsServiceUnavailableException(Throwable cause) {
        super("Сервис счетов временно недоступен", cause);
    }
}
