package ru.practicum.front.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class AccountShortDto {
    private String login;
    private String firstName;
    private String lastName;
}
