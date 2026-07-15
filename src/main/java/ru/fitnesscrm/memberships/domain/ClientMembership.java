package ru.fitnesscrm.memberships.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "client_memberships")
public class ClientMembership extends TenantEntity {

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MembershipTemplate template;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "membership_status")
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "remaining_classes")
    private Integer remainingClasses;

    @Column(name = "freeze_days_used", nullable = false)
    private Integer freezeDaysUsed = 0;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
}
