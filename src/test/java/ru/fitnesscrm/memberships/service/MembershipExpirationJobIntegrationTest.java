package ru.fitnesscrm.memberships.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;
import ru.fitnesscrm.support.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class MembershipExpirationJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClientMembershipRepository clientMembershipRepository;
    @Autowired
    private MembershipTemplateRepository membershipTemplateRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MembershipExpirationJob membershipExpirationJob;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Past-due ACTIVE membership becomes EXPIRED")
    void processExpiredMemberships_shouldExpireActivePastDue() {
        Fixture fixture = createFixture();
        ClientMembership pastActive = saveMembership(
                fixture,
                MembershipStatus.ACTIVE,
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(1)
        );

        int updated = membershipExpirationJob.processExpiredMemberships();

        entityManager.flush();
        entityManager.clear();

        ClientMembership reloaded = clientMembershipRepository.findById(pastActive.getId()).orElseThrow();
        assertTrue(updated >= 1);
        assertEquals(MembershipStatus.EXPIRED, reloaded.getStatus());
    }

    @Test
    @DisplayName("Past-due FROZEN membership becomes EXPIRED")
    void processExpiredMemberships_shouldExpireFrozenPastDue() {
        Fixture fixture = createFixture();
        ClientMembership pastFrozen = saveMembership(
                fixture,
                MembershipStatus.FROZEN,
                LocalDate.now().minusDays(40),
                LocalDate.now().minusDays(2)
        );

        membershipExpirationJob.processExpiredMemberships();
        entityManager.flush();
        entityManager.clear();

        assertEquals(
                MembershipStatus.EXPIRED,
                clientMembershipRepository.findById(pastFrozen.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    @DisplayName("Future ACTIVE membership is not expired")
    void processExpiredMemberships_shouldNotTouchFutureMembership() {
        Fixture fixture = createFixture();
        ClientMembership future = saveMembership(
                fixture,
                MembershipStatus.ACTIVE,
                LocalDate.now(),
                LocalDate.now().plusDays(20)
        );

        membershipExpirationJob.processExpiredMemberships();
        entityManager.flush();
        entityManager.clear();

        assertEquals(
                MembershipStatus.ACTIVE,
                clientMembershipRepository.findById(future.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    @DisplayName("Already DEPLETED / EXPIRED memberships are not changed")
    void processExpiredMemberships_shouldNotTouchDepletedOrAlreadyExpired() {
        Fixture fixture = createFixture();
        ClientMembership depleted = saveMembership(
                fixture,
                MembershipStatus.DEPLETED,
                LocalDate.now().minusDays(60),
                LocalDate.now().minusDays(5)
        );
        depleted.setRemainingClasses(0);
        clientMembershipRepository.save(depleted);

        ClientMembership alreadyExpired = saveMembership(
                fixture,
                MembershipStatus.EXPIRED,
                LocalDate.now().minusDays(90),
                LocalDate.now().minusDays(10)
        );

        membershipExpirationJob.processExpiredMemberships();
        entityManager.flush();
        entityManager.clear();

        assertEquals(
                MembershipStatus.DEPLETED,
                clientMembershipRepository.findById(depleted.getId()).orElseThrow().getStatus()
        );
        assertEquals(
                MembershipStatus.EXPIRED,
                clientMembershipRepository.findById(alreadyExpired.getId()).orElseThrow().getStatus()
        );
    }

    private Fixture createFixture() {
        Tenant tenant = new Tenant();
        tenant.setName("Expiration Gym");
        tenant.setSlug("expiration-gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        User client = new User();
        client.setTenant(tenant);
        client.setEmail("expire-client-" + System.nanoTime() + "@test.local");
        client.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        client.setRole(Role.CLIENT);
        client.setFirstName("Expire");
        client.setLastName("Client");
        client.setActive(true);
        client = userRepository.save(client);

        MembershipTemplate template = new MembershipTemplate();
        template.setTenantId(tenant.getId());
        template.setName("Monthly");
        template.setDescription("Test template");
        template.setPrice(BigDecimal.TEN);
        template.setActive(true);
        template.setClassLimit(10);
        template.setDurationDays(30);
        template = membershipTemplateRepository.save(template);

        return new Fixture(tenant, client, template);
    }

    private ClientMembership saveMembership(
            Fixture fixture,
            MembershipStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ClientMembership membership = new ClientMembership();
        membership.setTenantId(fixture.tenant().getId());
        membership.setClientId(fixture.client().getId());
        membership.setTemplate(fixture.template());
        membership.setStatus(status);
        membership.setRemainingClasses(5);
        membership.setFreezeDaysUsed(0);
        membership.setStartDate(startDate);
        membership.setEndDate(endDate);
        return clientMembershipRepository.save(membership);
    }

    private record Fixture(Tenant tenant, User client, MembershipTemplate template) {
    }
}
