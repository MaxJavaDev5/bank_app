package ru.practicum.transfer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("newBalanceOfSender")
    @JsonAlias("senderBalance")
    private BigDecimal newBalanceOfSender;
}
