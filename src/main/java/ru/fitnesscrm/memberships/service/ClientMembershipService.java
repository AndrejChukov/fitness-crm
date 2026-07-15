package ru.fitnesscrm.memberships.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.memberships.api.dto.request.AssignMembershipRequest;
import ru.fitnesscrm.memberships.api.dto.response.ClientMembershipResponse;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;

import java.time.LocalDate;

/**
 * Application service for memberships owned by individual clients.
 *
 * <p>The existing {@link #assign(AssignMembershipRequest)} operation creates an
 * ACTIVE membership from a template. {@link #hasActiveMembershipWithClasses(Long)}
 * exposes a module boundary used by Scheduling through {@code MembershipFacade}.
 *
 * <p>Learning exercise: complete the membership lifecycle here:
 * <ul>
 *     <li>freeze ACTIVE memberships and record {@code frozenAt};</li>
 *     <li>unfreeze them, consume at most 14 freeze days, and extend {@code endDate};</li>
 *     <li>deduct limited classes and move the membership to DEPLETED at zero;</li>
 *     <li>expire past-due memberships from a nightly scheduled job.</li>
 * </ul>
 */
@Service
@AllArgsConstructor
public class ClientMembershipService {

    private final ClientMembershipRepository membershipRepository;
    private final MembershipTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClientMembershipResponse assign(AssignMembershipRequest request) {
        Long tenantId = requireTenantId();
        MembershipTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership template not found: " + request.templateId()));

        userRepository.findById(request.clientId())
                .filter(user -> tenantId.equals(user.getTenantId()))
                .filter(user -> user.getRole() == Role.CLIENT)
                .orElseThrow(() -> new BusinessException("Client not found in current tenant"));

        LocalDate startDate = LocalDate.now();
        ClientMembership membership = new ClientMembership();
        membership.setTenantId(tenantId);
        membership.setClientId(request.clientId());
        membership.setTemplate(template);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRemainingClasses(template.getClassLimit());
        membership.setStartDate(startDate);
        membership.setEndDate(startDate.plusDays(template.getDurationDays()));

        return ClientMembershipResponse.from(membershipRepository.save(membership));
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMembershipWithClasses(Long clientId) {
        return membershipRepository.findFirstByClientIdAndStatusOrderByEndDateDesc(clientId, MembershipStatus.ACTIVE)
                .map(m -> m.getRemainingClasses() == null || m.getRemainingClasses() > 0)
                .orElse(false);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        return tenantId;
    }
}
