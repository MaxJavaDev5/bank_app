package ru.practicum.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CashOperationDto {

    @NotNull(message = "Сумма не может быть null")
    @Positive(message = "Сумма должна быть больше нуля")
    private BigDecimal amount;
}
