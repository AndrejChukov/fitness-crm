package ru.fitnesscrm.memberships.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.memberships.api.dto.request.CreateMembershipTemplateRequest;
import ru.fitnesscrm.memberships.api.dto.response.MembershipTemplateResponse;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class MembershipTemplateService {

    private final MembershipTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<MembershipTemplateResponse> findAllActive() {
        return templateRepository.findByActiveTrue().stream()
                .map(MembershipTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipTemplateResponse findById(Long id) {
        return templateRepository.findById(id)
                .map(MembershipTemplateResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Membership template not found: " + id));
    }

    @Transactional
    public MembershipTemplateResponse create(CreateMembershipTemplateRequest request) {
        MembershipTemplate template = new MembershipTemplate();
        template.setTenantId(requireTenantId());
        template.setName(request.name());
        template.setDescription(request.description());
        template.setPrice(request.price());
        template.setClassLimit(request.classLimit());
        template.setDurationDays(request.durationDays());
        template.setActive(true);
        return MembershipTemplateResponse.from(templateRepository.save(template));
    }

    private Long requireTenantId() {
        return TenantContext.getTenantId();
    }
}
