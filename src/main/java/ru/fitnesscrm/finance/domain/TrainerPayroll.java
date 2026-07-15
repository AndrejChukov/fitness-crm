package ru.fitnesscrm.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "trainer_payroll")
public class TrainerPayroll extends TenantEntity {

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "class_session_id", nullable = false)
    private Long classSessionId;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "bonus_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payroll_status")
    private PayrollStatus status = PayrollStatus.PENDING;

    @Column(name = "calculated_at")
    private Instant calculatedAt;
}
