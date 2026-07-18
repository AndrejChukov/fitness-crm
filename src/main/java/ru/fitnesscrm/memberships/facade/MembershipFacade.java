package ru.fitnesscrm.memberships.facade;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.memberships.service.ClientMembershipService;

@Component
@AllArgsConstructor
public class MembershipFacade {

    private final ClientMembershipService clientMembershipService;

    public boolean canBookClasses(Long clientId) {
        return clientMembershipService.hasActiveMembershipWithClasses(clientId);
    }

    /** Deducts one class from the client's active membership (used by late cancel / no-show). */
    public void deductClassForClient(Long clientId) {
        clientMembershipService.deductClassForClient(clientId);
    }
}
