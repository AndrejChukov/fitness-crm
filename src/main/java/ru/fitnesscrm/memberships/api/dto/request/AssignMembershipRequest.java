package ru.fitnesscrm.memberships.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignMembershipRequest(
        @NotNull Long clientId,
        @NotNull Long templateId
) {
}
