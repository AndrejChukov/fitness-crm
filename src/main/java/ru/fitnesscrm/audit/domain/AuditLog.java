package ru.fitnesscrm.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * TODO (your exercise): wire Hibernate Envers or @EntityListeners so status changes
 * (e.g. Invoice UNPAID -> PAID) and deletions are written here automatically.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_action")
    private AuditAction action;

    @Column(name = "changed_by_user")
    private Long changedByUser;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
