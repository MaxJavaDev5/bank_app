package ru.practicum.transfer.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AccountDto {
    private String login;
    private String firstName;
    private String lastName;
    private BigDecimal balance;
}
