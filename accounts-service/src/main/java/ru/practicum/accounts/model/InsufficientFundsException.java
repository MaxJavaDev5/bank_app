package ru.practicum.accounts.model;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String login, BigDecimal balance, BigDecimal amount) {
        super("На счёте '" + login + "' недостаточно средств. " +
              "Баланс: " + balance + ", запрошено: " + amount);
    }
}
