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
@Table(name = "transactions")
public class Transaction extends TenantEntity {

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_method")
    private PaymentMethod method;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();
}
