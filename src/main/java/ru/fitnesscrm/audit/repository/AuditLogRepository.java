package ru.fitnesscrm.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.audit.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
