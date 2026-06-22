package ru.practicum.cash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CashOperationDto {

    @NotBlank(message = "Логин не может быть пустым")
    private String login;

    @NotNull(message = "Сумма не может быть null")
    @Positive(message = "Сумма должна быть больше нуля")
    private BigDecimal amount;
}
