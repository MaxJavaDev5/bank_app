package ru.practicum.front.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CashForm {

    @NotNull(message = "Сумма не может быть пустой")
    @Positive(message = "Сумма должна быть больше нуля")
    private BigDecimal amount;

    @NotNull(message = "Не выбрано действие")
    private CashAction action;
}
