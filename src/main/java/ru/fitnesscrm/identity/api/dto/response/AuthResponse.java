package ru.fitnesscrm.identity.api.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        Long tenantId,
        String email,
        String role
) {
}
