package ru.fitnesscrm.scheduling.api.dto.response;

import ru.fitnesscrm.scheduling.domain.Facility;

import java.time.Instant;

public record FacilityResponse(
        Long id,
        Long tenantId,
        String name,
        Integer capacity,
        boolean active,
        Instant createdAt
) {
    public static FacilityResponse from(Facility facility) {
        return new FacilityResponse(
                facility.getId(),
                facility.getTenantId(),
                facility.getName(),
                facility.getCapacity(),
                facility.isActive(),
                facility.getCreatedAt()
        );
    }
}
