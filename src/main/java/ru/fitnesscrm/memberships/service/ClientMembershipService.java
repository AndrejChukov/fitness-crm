package ru.fitnesscrm.memberships.service;

import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.facade.FinanceFacade;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.facade.IdentityFacade;
import ru.fitnesscrm.memberships.api.dto.request.AssignMembershipRequest;
import ru.fitnesscrm.memberships.api.dto.response.ClientMembershipResponse;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.mapper.MembershipMapper;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Application service for memberships owned by individual clients.
 *
 * <p>Freeze/unfreeze policy uses a 14-day freeze budget with <b>cap</b> semantics:
 * if a freeze lasted longer than the remaining allowance, only the remaining days
 * are charged and the membership is always unfrozen (never stuck in {@code FROZEN}).
 */
@Service
@AllArgsConstructor
public class ClientMembershipService {

    private static final int MAX_FREEZE_DAYS = 14;

    private final FinanceFacade financeFacade;
    private final IdentityFacade identityFacade;

    private final ClientMembershipRepository membershipRepository;
    private final MembershipTemplateRepository templateRepository;
    private final MembershipMapper membershipMapper;

    @Transactional
    public ClientMembershipResponse assign(AssignMembershipRequest request) {
        Long tenantId = requireTenantId();
        MembershipTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership template not found: " + request.templateId()));

        identityFacade.requireClientInTenant(request.clientId(), tenantId);

        LocalDate startDate = LocalDate.now();
        ClientMembership membership = new ClientMembership();
        membership.setTenantId(tenantId);
        membership.setClientId(request.clientId());
        membership.setTemplate(template);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRemainingClasses(template.getClassLimit());
        membership.setStartDate(startDate);
        membership.setEndDate(startDate.plusDays(template.getDurationDays()));

        ClientMembership savedMembership = membershipRepository.save(membership);

        financeFacade.createInvoiceForMembership(savedMembership.getId(), savedMembership.getClientId(), template.getPrice());

        return membershipMapper.toClientMembershipResponse(savedMembership);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMembershipWithClasses(Long clientId) {
        return membershipRepository.findFirstByClientIdAndStatusOrderByEndDateDesc(clientId, MembershipStatus.ACTIVE)
                .map(m -> m.getRemainingClasses() == null || m.getRemainingClasses() > 0)
                .orElse(false);
    }

    @Transactional
    public ClientMembershipResponse freeze(Long membershipId) {
        ClientMembership clientMembership = requireAccessibleMembership(membershipId);

        if (clientMembership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE memberships can be frozen");
        }
        if (clientMembership.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Your membership is expired");
        }
        if (clientMembership.getFreezeDaysUsed() >= MAX_FREEZE_DAYS) {
            throw new BusinessException("You have already used the maximum number of freeze days");
        }
        if (clientMembership.getFrozenAt() != null) {
            throw new BusinessException("Membership is already frozen");
        }

        clientMembership.setStatus(MembershipStatus.FROZEN);
        clientMembership.setFrozenAt(Instant.now());

        return membershipMapper.toClientMembershipResponse(clientMembership);
    }

    @Transactional
    public ClientMembershipResponse unfreeze(Long membershipId) {
        ClientMembership clientMembership = requireAccessibleMembership(membershipId);

        if (clientMembership.getStatus() != MembershipStatus.FROZEN
                || clientMembership.getFrozenAt() == null) {
            throw new BusinessException("Frozen membership not found for client");
        }

        int freezeDaysUsed = clientMembership.getFreezeDaysUsed();
        long days = Duration.between(clientMembership.getFrozenAt(), Instant.now()).toDays();
        if (days == 0) {
            days = 1;
        }
        long allowanceDays = Math.max(0, MAX_FREEZE_DAYS - freezeDaysUsed);
        int chargedDays = (int) Math.min(allowanceDays, days);

        clientMembership.setFreezeDaysUsed(freezeDaysUsed + chargedDays);
        clientMembership.setEndDate(clientMembership.getEndDate().plusDays(chargedDays));
        clientMembership.setFrozenAt(null);

        Integer remainingClasses = clientMembership.getRemainingClasses();
        if (remainingClasses != null && remainingClasses == 0) {
            clientMembership.setStatus(MembershipStatus.DEPLETED);
        } else {
            clientMembership.setStatus(MembershipStatus.ACTIVE);
        }

        return membershipMapper.toClientMembershipResponse(clientMembership);
    }

    @Transactional
    public void deductClassForClient(Long clientId) {
        ClientMembership membership = membershipRepository.findFirstByClientIdAndStatusOrderByEndDateDesc(clientId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        deductClass(membership.getId());
    }

    /**
     * Deducts one class from a limited membership.
     * Unlimited memberships ({@code remainingClasses == null}) are left unchanged.
     * Uses entity {@code @Version} so concurrent deductions cannot drive the counter negative.
     */
    @Transactional
    public ClientMembershipResponse deductClass(Long membershipId) {
        ClientMembership clientMembership = requireAccessibleMembership(membershipId);

        if (clientMembership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE memberships can have classes deducted");
        }

        Integer remainingClasses = clientMembership.getRemainingClasses();
        if (remainingClasses == null) {
            return membershipMapper.toClientMembershipResponse(clientMembership);
        }
        if (remainingClasses <= 0) {
            throw new BusinessException("No remaining classes to deduct");
        }

        int next = remainingClasses - 1;
        clientMembership.setRemainingClasses(next);
        if (next == 0) {
            clientMembership.setStatus(MembershipStatus.DEPLETED);
        }

        return membershipMapper.toClientMembershipResponse(clientMembership);
    }

    @Transactional(readOnly = true)
    public List<ClientMembershipResponse> findClientMemberships(Long clientId) {
        requireTenantId();
        if (Role.CLIENT.equals(TenantContext.getRole())
                && !TenantContext.getUserId().equals(clientId)) {
            throw new AccessDeniedException("Access denied to this client's memberships");
        }

        return membershipMapper.toClientMembershipResponseList(
                membershipRepository.findByClientIdAndStatus(clientId, MembershipStatus.ACTIVE)
        );
    }

    /**
     * Loads a membership and enforces isolation:
     * <ul>
     *     <li>other tenant → 404 ({@link ResourceNotFoundException})</li>
     *     <li>CLIENT accessing another client's membership → 403 ({@link AccessDeniedException})</li>
     *     <li>TENANT_ADMIN / TRAINER may manage any membership in their tenant</li>
     * </ul>
     */
    private ClientMembership requireAccessibleMembership(Long membershipId) {
        Long tenantId = requireTenantId();
        ClientMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + membershipId));

        if (!tenantId.equals(membership.getTenantId())) {
            throw new ResourceNotFoundException("Membership not found: " + membershipId);
        }

        if (Role.CLIENT.equals(TenantContext.getRole())
                && !TenantContext.getUserId().equals(membership.getClientId())) {
            throw new AccessDeniedException("Access denied to this membership");
        }

        return membership;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        return tenantId;
    }
}
