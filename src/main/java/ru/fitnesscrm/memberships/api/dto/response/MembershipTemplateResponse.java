package ru.fitnesscrm.memberships.api.dto.response;

import ru.fitnesscrm.memberships.domain.MembershipTemplate;

import java.math.BigDecimal;
import java.time.Instant;

public record MembershipTemplateResponse(
        Long id,
        Long tenantId,
        String name,
        String description,
        BigDecimal price,
        Integer classLimit,
        Integer durationDays,
        boolean active,
        Instant createdAt
) {
    public static MembershipTemplateResponse from(MembershipTemplate template) {
        return new MembershipTemplateResponse(
                template.getId(),
                template.getTenantId(),
                template.getName(),
                template.getDescription(),
                template.getPrice(),
                template.getClassLimit(),
                template.getDurationDays(),
                template.isActive(),
                template.getCreatedAt()
        );
    }
}
