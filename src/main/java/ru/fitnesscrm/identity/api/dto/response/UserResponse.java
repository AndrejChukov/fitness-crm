package ru.fitnesscrm.identity.api.dto.response;

import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        Long tenantId,
        String email,
        Role role,
        String firstName,
        String lastName,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
