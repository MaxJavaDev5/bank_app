package ru.practicum.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TransferDto {

    @NotBlank(message = "Логин получателя не может быть пустым")
    private String toLogin;

    @NotNull(message = "Сумма не может быть null")
    @Positive(message = "Сумма должна быть больше нуля")
    private BigDecimal amount;
}
