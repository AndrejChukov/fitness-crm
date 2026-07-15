package ru.fitnesscrm.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.fitnesscrm.identity.domain.Role;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Role role,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName
) {
}
