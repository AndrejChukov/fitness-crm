package ru.fitnesscrm.memberships.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;

/**
 * Nightly job that expires past-due memberships across all tenants.
 * Runs without JWT / TenantContext — uses {@link TenantContext#executeWithoutFilter}.
 */
@Component
@AllArgsConstructor
@Slf4j
public class MembershipExpirationJob {

    private final ClientMembershipRepository clientMembershipRepository;

    @Scheduled(cron = "0 1 0 * * *", zone = "UTC")
    @Transactional
    public int processExpiredMemberships() {
        log.info("Membership expiration cron started");

        int updated = TenantContext.executeWithoutFilter(() ->
                clientMembershipRepository.updateAllStatus(
                        MembershipStatus.EXPIRED,
                        MembershipStatus.ACTIVE,
                        MembershipStatus.FROZEN
                ));

        log.info("Expired {} membership(s)", updated);
        return updated;
    }
}
