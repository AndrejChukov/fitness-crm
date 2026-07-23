package ru.fitnesscrm.scheduling.facade;

/** Cross-module view of an ended class session (no JPA entity leak). */
public record EndedClassSessionView(Long id, Long tenantId, Long trainerId) {
}
