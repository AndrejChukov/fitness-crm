package ru.fitnesscrm.scheduling.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull Long classSessionId,
        @NotNull Long clientId
) {
}
