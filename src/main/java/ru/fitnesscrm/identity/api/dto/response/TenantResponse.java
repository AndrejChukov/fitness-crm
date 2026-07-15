package ru.fitnesscrm.identity.api.dto.response;

import ru.fitnesscrm.identity.domain.Tenant;

import java.time.Instant;

public record TenantResponse(
        Long id,
        String name,
        String slug,
        boolean active,
        Instant createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.isActive(),
                tenant.getCreatedAt()
        );
    }
}
