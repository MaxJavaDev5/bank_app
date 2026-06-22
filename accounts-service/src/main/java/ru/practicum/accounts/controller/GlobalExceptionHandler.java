package ru.practicum.accounts.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.accounts.dto.ErrorResponse;
import ru.practicum.accounts.model.AccountNotFoundException;
import ru.practicum.accounts.model.InsufficientFundsException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Аккаунт не найден: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Недостаточно средств: {}", ex.getMessage());
        return  new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("Ошибка валидации");
        log.warn("Ошибка валидации: {}", errorMessage);
        return new ErrorResponse(errorMessage);
    }
}
