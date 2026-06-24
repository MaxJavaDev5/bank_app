package ru.practicum.accounts.model;

public class TransferException extends RuntimeException {

    public TransferException(String message) {
        super(message);
    }
}
