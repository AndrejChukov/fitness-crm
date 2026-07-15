package ru.fitnesscrm.scheduling.api.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateClassSessionRequest(
        @NotNull Long facilityId,
        @NotNull Long trainerId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull @Future Instant startTime,
        @NotNull @Future Instant endTime,
        @NotNull @Min(1) Integer maxCapacity
) {
}
