package ru.practicum.transfer.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {
    private String fromLogin;
    private String toLogin;
    private BigDecimal amount;
    private BigDecimal newBalanceOfSender;
}
