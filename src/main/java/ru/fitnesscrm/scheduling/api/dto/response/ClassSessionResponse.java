package ru.fitnesscrm.scheduling.api.dto.response;

import ru.fitnesscrm.scheduling.domain.ClassSession;

import java.time.Instant;

public record ClassSessionResponse(
        Long id,
        Long tenantId,
        Long facilityId,
        Long trainerId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        Integer maxCapacity,
        Long version,
        Instant createdAt
) {
    public static ClassSessionResponse from(ClassSession session) {
        return new ClassSessionResponse(
                session.getId(),
                session.getTenantId(),
                session.getFacilityId(),
                session.getTrainerId(),
                session.getTitle(),
                session.getDescription(),
                session.getStartTime(),
                session.getEndTime(),
                session.getMaxCapacity(),
                session.getVersion(),
                session.getCreatedAt()
        );
    }
}
