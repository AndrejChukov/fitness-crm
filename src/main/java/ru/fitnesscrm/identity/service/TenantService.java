package ru.fitnesscrm.identity.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.identity.api.dto.request.CreateTenantRequest;
import ru.fitnesscrm.identity.api.dto.response.TenantResponse;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.repository.TenantRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<TenantResponse> findAll() {
        return tenantRepository.findAll().stream()
                .map(TenantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse findById(Long id) {
        return tenantRepository.findById(id)
                .map(TenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Tenant slug already exists: " + request.slug());
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setActive(true);
        return TenantResponse.from(tenantRepository.save(tenant));
    }
}
