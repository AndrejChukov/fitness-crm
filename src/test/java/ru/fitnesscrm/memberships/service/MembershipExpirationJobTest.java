package ru.fitnesscrm.memberships.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipExpirationJobTest {

    @Mock
    private ClientMembershipRepository clientMembershipRepository;

    @InjectMocks
    private MembershipExpirationJob membershipExpirationJob;

    @Test
    @DisplayName("Job delegates bulk update ACTIVE/FROZEN → EXPIRED")
    void processExpiredMemberships_shouldCallBulkUpdate() {
        when(clientMembershipRepository.updateAllStatus(
                MembershipStatus.EXPIRED,
                MembershipStatus.ACTIVE,
                MembershipStatus.FROZEN
        )).thenReturn(3);

        int updated = membershipExpirationJob.processExpiredMemberships();

        assertEquals(3, updated);
        verify(clientMembershipRepository).updateAllStatus(
                MembershipStatus.EXPIRED,
                MembershipStatus.ACTIVE,
                MembershipStatus.FROZEN
        );
    }
}
