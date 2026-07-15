package ru.fitnesscrm.config;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.common.tenant.TenantContext;

@Aspect
@Component
@AllArgsConstructor
public class TenantFilterAspect {

    private final EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void enableTenantFilter() {
        if (!TenantContext.shouldApplyTenantFilter()) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter")
                .setParameter("tenantId", TenantContext.getTenantId());
    }
}
