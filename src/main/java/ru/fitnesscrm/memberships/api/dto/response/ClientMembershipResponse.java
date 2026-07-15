package ru.fitnesscrm.memberships.api.dto.response;

import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ClientMembershipResponse(
        Long id,
        Long tenantId,
        Long clientId,
        Long templateId,
        String templateName,
        MembershipStatus status,
        Integer remainingClasses,
        Integer freezeDaysUsed,
        Instant frozenAt,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt
) {
    public static ClientMembershipResponse from(ClientMembership membership) {
        return new ClientMembershipResponse(
                membership.getId(),
                membership.getTenantId(),
                membership.getClientId(),
                membership.getTemplate().getId(),
                membership.getTemplate().getName(),
                membership.getStatus(),
                membership.getRemainingClasses(),
                membership.getFreezeDaysUsed(),
                membership.getFrozenAt(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getCreatedAt()
        );
    }
}
