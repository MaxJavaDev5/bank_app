package ru.practicum.front.dto;

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
public class TransferForm {

    @NotBlank(message = "Выберите получателя")
    private String toLogin;

    @NotNull(message = "Сумма не может быть пустой")
    @Positive(message = "Сумма должна быть больше нуля")
    private BigDecimal amount;
}
