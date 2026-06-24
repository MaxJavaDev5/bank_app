package ru.practicum.accounts.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AccountDto {
    private Long id;
    private String login;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private BigDecimal balance;
}
