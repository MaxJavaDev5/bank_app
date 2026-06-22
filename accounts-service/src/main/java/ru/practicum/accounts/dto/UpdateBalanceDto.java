package ru.practicum.accounts.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class UpdateBalanceDto {

    @NotNull(message = "Сумма не может быть null")
    @Positive(message = "Сумма должна быть больше нуля")
    private java.math.BigDecimal amount;

    @NotNull(message = "Тип операции не может быть null")
    private OperationType operationType;

    public enum OperationType {
        DEPOSIT,   
        WITHDRAW   
    }
}
