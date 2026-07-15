package ru.fitnesscrm.memberships.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMembershipTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @Min(1) Integer classLimit,
        @NotNull @Min(1) Integer durationDays
) {
}
