package ru.practicum.transfer.model;

public class RemoteException extends RuntimeException {

    public RemoteException(String service, String response) {
        super("Ошибка от " + service + ": " + response);
    }
}
