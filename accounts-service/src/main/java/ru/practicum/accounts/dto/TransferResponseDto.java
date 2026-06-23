package ru.practicum.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {

    private String fromLogin;
    private String toLogin;
    private BigDecimal amount;
    private BigDecimal senderBalance;
}
