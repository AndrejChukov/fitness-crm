package ru.fitnesscrm.memberships.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;

import java.util.List;
import java.util.Optional;

public interface ClientMembershipRepository extends JpaRepository<ClientMembership, Long> {

    List<ClientMembership> findByClientIdAndStatus(Long clientId, MembershipStatus status);

    ClientMembership findByClientIdAndTemplateIdAndStatus(Long clientId, Long templateId, MembershipStatus status);

    Optional<ClientMembership> findFirstByClientIdAndStatusOrderByEndDateDesc(
            Long clientId,
            MembershipStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ClientMembership cm
            SET cm.status = :expired
            WHERE cm.endDate < CURRENT_DATE
              AND cm.status IN (:active, :frozen)
            """)
    int updateAllStatus(MembershipStatus expired, MembershipStatus active, MembershipStatus frozen);

}
