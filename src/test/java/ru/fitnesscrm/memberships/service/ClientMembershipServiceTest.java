package ru.fitnesscrm.memberships.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.mapstruct.factory.Mappers;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.memberships.api.dto.response.ClientMembershipResponse;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.mapper.MembershipMapper;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientMembershipServiceTest {

    private static final Long TENANT_ID = 100L;
    private static final Long OTHER_TENANT_ID = 200L;
    private static final Long CLIENT_ID = 1L;
    private static final Long OTHER_CLIENT_ID = 2L;
    private static final Long STAFF_USER_ID = 50L;
    private static final Long MEMBERSHIP_ID = 10L;

    @Mock
    private ClientMembershipRepository membershipRepository;
    @Mock
    private MembershipTemplateRepository templateRepository;
    @Mock
    private UserRepository userRepository;

    private final MembershipMapper membershipMapper = Mappers.getMapper(MembershipMapper.class);

    private ClientMembershipService clientMembershipService;

    private ClientMembership membership;
    private MembershipTemplate template;

    @BeforeEach
    void setUp() {
        clientMembershipService = new ClientMembershipService(
                membershipRepository,
                templateRepository,
                userRepository,
                membershipMapper
        );

        template = new MembershipTemplate();
        template.setId(1L);
        template.setName("8 Classes / Month");

        membership = new ClientMembership();
        membership.setId(MEMBERSHIP_ID);
        membership.setTenantId(TENANT_ID);
        membership.setClientId(CLIENT_ID);
        membership.setTemplate(template);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRemainingClasses(8);
        membership.setFreezeDaysUsed(0);
        membership.setStartDate(LocalDate.now());
        membership.setEndDate(LocalDate.now().plusDays(30));
        membership.setFrozenAt(null);
        membership.setVersion(0L);

        TenantContext.set(TENANT_ID, CLIENT_ID, Role.CLIENT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Happy path: freeze ACTIVE membership")
    void freeze_shouldFreezeMembership_whenDataIsValid() {
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.freeze(MEMBERSHIP_ID);

        assertEquals(MembershipStatus.FROZEN, response.status());
        assertNotNull(response.frozenAt());
        assertEquals(MembershipStatus.FROZEN, membership.getStatus());
        assertNotNull(membership.getFrozenAt());
    }

    @Test
    @DisplayName("Happy path: unfreeze FROZEN membership and extend endDate")
    void unfreeze_shouldRestoreActiveMembership_whenDataIsValid() {
        LocalDate originalEndDate = membership.getEndDate();
        membership.setStatus(MembershipStatus.FROZEN);
        membership.setFrozenAt(Instant.now().minus(5, ChronoUnit.DAYS));
        membership.setFreezeDaysUsed(0);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.unfreeze(MEMBERSHIP_ID);

        assertEquals(MembershipStatus.ACTIVE, response.status());
        assertNull(response.frozenAt());
        assertEquals(5, response.freezeDaysUsed());
        assertEquals(originalEndDate.plusDays(5), response.endDate());
    }

    @Test
    @DisplayName("Cannot freeze membership with EXPIRED status")
    void freeze_shouldReject_whenStatusIsExpired() {
        membership.setStatus(MembershipStatus.EXPIRED);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
        assertEquals(MembershipStatus.EXPIRED, membership.getStatus());
        assertNull(membership.getFrozenAt());
    }

    @Test
    @DisplayName("Cannot freeze membership with DEPLETED status")
    void freeze_shouldReject_whenStatusIsDepleted() {
        membership.setStatus(MembershipStatus.DEPLETED);
        membership.setRemainingClasses(0);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
        assertEquals(MembershipStatus.DEPLETED, membership.getStatus());
        assertNull(membership.getFrozenAt());
    }

    @Test
    @DisplayName("Cap scenario: long freeze charges only remaining allowance; membership leaves FROZEN")
    void unfreeze_shouldCapChargedDays_whenFreezeWouldExceedFourteenDays() {
        LocalDate originalEndDate = membership.getEndDate();
        membership.setStatus(MembershipStatus.FROZEN);
        membership.setFreezeDaysUsed(10);
        membership.setFrozenAt(Instant.now().minus(10, ChronoUnit.DAYS));
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.unfreeze(MEMBERSHIP_ID);

        assertEquals(MembershipStatus.ACTIVE, response.status());
        assertEquals(14, response.freezeDaysUsed());
        assertEquals(originalEndDate.plusDays(4), response.endDate());
        assertNull(response.frozenAt());
    }

    @Test
    @DisplayName("Cannot start freeze when freeze budget is already exhausted")
    void freeze_shouldReject_whenFreezeDaysUsedAlreadyAtLimit() {
        membership.setFreezeDaysUsed(14);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
    }

    @Test
    @DisplayName("Unlimited membership (remainingClasses == null) returns to ACTIVE after unfreeze")
    void unfreeze_shouldReturnToActive_whenRemainingClassesIsNull() {
        membership.setStatus(MembershipStatus.FROZEN);
        membership.setRemainingClasses(null);
        membership.setFrozenAt(Instant.now().minus(3, ChronoUnit.DAYS));
        membership.setFreezeDaysUsed(0);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.unfreeze(MEMBERSHIP_ID);

        assertEquals(MembershipStatus.ACTIVE, response.status());
        assertNull(response.remainingClasses());
        assertEquals(3, response.freezeDaysUsed());
        assertNull(response.frozenAt());
    }

    @Test
    @DisplayName("Tenant isolation: membership from another tenant → 404")
    void freeze_shouldReturnNotFound_whenMembershipBelongsToAnotherTenant() {
        membership.setTenantId(OTHER_TENANT_ID);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(ResourceNotFoundException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
    }

    @Test
    @DisplayName("Client isolation: another client's membership → 403")
    void freeze_shouldDenyAccess_whenClientTriesToFreezeSomeoneElse() {
        membership.setClientId(OTHER_CLIENT_ID);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(AccessDeniedException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
    }

    @Test
    @DisplayName("Staff can freeze any membership in their tenant")
    void freeze_shouldAllowStaff_whenMembershipBelongsToClientInSameTenant() {
        TenantContext.set(TENANT_ID, STAFF_USER_ID, Role.TENANT_ADMIN);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.freeze(MEMBERSHIP_ID);

        assertEquals(MembershipStatus.FROZEN, response.status());
    }

    @Test
    @DisplayName("Unfreeze with zero remaining classes becomes DEPLETED")
    void unfreeze_shouldBecomeDepleted_whenRemainingClassesIsZero() {
        membership.setStatus(MembershipStatus.FROZEN);
        membership.setRemainingClasses(0);
        membership.setFrozenAt(Instant.now().minus(2, ChronoUnit.DAYS));
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.unfreeze(MEMBERSHIP_ID);

        assertEquals(MembershipStatus.DEPLETED, response.status());
        assertEquals(0, response.remainingClasses());
    }

    @Test
    void freeze_shouldThrowException_whenMembershipNotFound() {
        when(membershipRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
    }

    @Test
    void freeze_shouldThrowException_whenEndDateIsInThePast() {
        membership.setEndDate(LocalDate.now().minusDays(1));
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.freeze(MEMBERSHIP_ID));
    }

    @Test
    void unfreeze_shouldReject_whenMembershipIsNotFrozen() {
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setFrozenAt(null);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.unfreeze(MEMBERSHIP_ID));
    }

    @Test
    @DisplayName("deductClass: decreases remaining classes by one")
    void deductClass_shouldDecreaseRemainingClasses() {
        membership.setRemainingClasses(3);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.deductClass(MEMBERSHIP_ID);

        assertEquals(2, response.remainingClasses());
        assertEquals(MembershipStatus.ACTIVE, response.status());
    }

    @Test
    @DisplayName("deductClass: last class → DEPLETED and booking check fails")
    void deductClass_shouldDeplete_whenLastClassIsDeducted() {
        membership.setRemainingClasses(1);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
        when(membershipRepository.findFirstByClientIdAndStatusOrderByEndDateDesc(CLIENT_ID, MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ClientMembershipResponse response = clientMembershipService.deductClass(MEMBERSHIP_ID);

        assertEquals(0, response.remainingClasses());
        assertEquals(MembershipStatus.DEPLETED, response.status());
        assertFalse(clientMembershipService.hasActiveMembershipWithClasses(CLIENT_ID));
    }

    @Test
    @DisplayName("deductClass: unlimited membership stays ACTIVE with null remainingClasses")
    void deductClass_shouldKeepUnlimitedMembershipActive() {
        membership.setRemainingClasses(null);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        ClientMembershipResponse response = clientMembershipService.deductClass(MEMBERSHIP_ID);

        assertNull(response.remainingClasses());
        assertEquals(MembershipStatus.ACTIVE, response.status());
    }

    @Test
    @DisplayName("deductClass: rejects when remainingClasses is already 0")
    void deductClass_shouldReject_whenNoClassesLeft() {
        membership.setRemainingClasses(0);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.deductClass(MEMBERSHIP_ID));
    }

    @Test
    @DisplayName("deductClass: rejects non-ACTIVE membership")
    void deductClass_shouldReject_whenStatusIsFrozen() {
        membership.setStatus(MembershipStatus.FROZEN);
        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThrows(BusinessException.class, () -> clientMembershipService.deductClass(MEMBERSHIP_ID));
    }

    @Test
    @DisplayName("findClientMemberships: client cannot list another client's memberships")
    void findClientMemberships_shouldDenyAccess_whenClientRequestsSomeoneElse() {
        assertThrows(
                AccessDeniedException.class,
                () -> clientMembershipService.findClientMemberships(OTHER_CLIENT_ID)
        );
    }

    @Test
    @DisplayName("findClientMemberships: returns ACTIVE memberships for requested client")
    void findClientMemberships_shouldReturnActiveMemberships() {
        when(membershipRepository.findByClientIdAndStatus(CLIENT_ID, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));

        List<ClientMembershipResponse> responses = clientMembershipService.findClientMemberships(CLIENT_ID);

        assertEquals(1, responses.size());
        assertEquals(MEMBERSHIP_ID, responses.getFirst().id());
        assertEquals("8 Classes / Month", responses.getFirst().templateName());
    }
}
