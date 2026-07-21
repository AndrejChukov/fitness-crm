package ru.fitnesscrm.audit.listener;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.audit.domain.AuditAction;
import ru.fitnesscrm.audit.domain.AuditLog;
import ru.fitnesscrm.audit.repository.AuditLogRepository;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.memberships.domain.ClientMembership;

import java.util.Objects;

/**
 * JPA listener: writes {@link AuditLog} on Invoice/ClientMembership status changes and membership deletes.
 * Requires {@code @PostLoad} to populate {@code originalStatus} before update.
 */
@Component
@Slf4j
public class AuditEntityListener {

    private final AuditLogRepository auditLogRepository;

    public AuditEntityListener(@Lazy AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof Invoice invoice) {
            auditStatusChange(
                    "Invoice",
                    invoice.getId(),
                    invoice.getOriginalStatus() == null ? null : invoice.getOriginalStatus().name(),
                    invoice.getStatus() == null ? null : invoice.getStatus().name(),
                    invoice.getOriginalStatus(),
                    invoice.getStatus()
            );
        } else if (entity instanceof ClientMembership membership) {
            auditStatusChange(
                    "ClientMembership",
                    membership.getId(),
                    membership.getOriginalStatus() == null ? null : membership.getOriginalStatus().name(),
                    membership.getStatus() == null ? null : membership.getStatus().name(),
                    membership.getOriginalStatus(),
                    membership.getStatus()
            );
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (entity instanceof Invoice invoice) {
            invoice.setOriginalStatus(invoice.getStatus());
        } else if (entity instanceof ClientMembership membership) {
            membership.setOriginalStatus(membership.getStatus());
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        if (!(entity instanceof ClientMembership membership)) {
            return;
        }

        log.info("Auditing deletion of ClientMembership ID: {}", membership.getId());

        AuditLog audit = new AuditLog();
        audit.setTenantId(TenantContext.getTenantId());
        audit.setEntityName("ClientMembership");
        audit.setEntityId(membership.getId());
        audit.setAction(AuditAction.DELETE);
        audit.setChangedByUser(TenantContext.getUserId());
        auditLogRepository.save(audit);
    }

    private void auditStatusChange(
            String entityName,
            Long entityId,
            String oldValue,
            String newValue,
            Object originalStatus,
            Object newStatus
    ) {
        if (originalStatus == null || Objects.equals(originalStatus, newStatus)) {
            return;
        }

        log.info("Auditing status change for {} ID {}: {} -> {}", entityName, entityId, oldValue, newValue);

        AuditLog audit = new AuditLog();
        audit.setTenantId(TenantContext.getTenantId());
        audit.setAction(AuditAction.UPDATE);
        audit.setChangedByUser(TenantContext.getUserId());
        audit.setEntityName(entityName);
        audit.setEntityId(entityId);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        auditLogRepository.save(audit);
    }
}
