package ru.practicum.front.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import ru.practicum.front.validation.MinimumAge;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAccountForm {

    @NotBlank(message = "Имя не может быть пустым")
    private String firstName;

    @NotBlank(message = "Фамилия не может быть пустой")
    private String lastName;

    @NotNull(message = "Дата рождения не может быть пустой")
    @MinimumAge(18)
    private LocalDate birthDate;
}
