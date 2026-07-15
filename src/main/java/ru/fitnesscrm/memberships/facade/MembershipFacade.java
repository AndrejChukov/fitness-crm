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
}
