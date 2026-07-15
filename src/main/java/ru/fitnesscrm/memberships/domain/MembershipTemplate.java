package ru.fitnesscrm.memberships.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "membership_templates")
public class MembershipTemplate extends TenantEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "class_limit")
    private Integer classLimit;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private boolean active = true;
}
