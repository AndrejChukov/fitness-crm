package ru.fitnesscrm.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import ru.fitnesscrm.common.tenant.TenantContext;

@Getter
@Setter
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @PrePersist
    protected void assignTenantId() {
        if (tenantId == null && TenantContext.getTenantId() != null) {
            tenantId = TenantContext.getTenantId();
        }
    }
}
