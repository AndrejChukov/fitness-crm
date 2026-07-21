package ru.fitnesscrm.finance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fitnesscrm.audit.listener.AuditEntityListener;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "invoices")
@EntityListeners(AuditEntityListener.class)
public class Invoice extends TenantEntity {

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_membership_id")
    private Long clientMembershipId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "invoice_status")
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Transient
    private InvoiceStatus originalStatus;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @PostLoad
    public void postLoad() {
        this.originalStatus = this.status;
    }
}
