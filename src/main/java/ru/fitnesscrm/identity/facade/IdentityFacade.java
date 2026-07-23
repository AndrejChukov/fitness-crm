package ru.fitnesscrm.identity.facade;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.repository.UserRepository;

@Component
@AllArgsConstructor
public class IdentityFacade {

    private final UserRepository userRepository;

    /** Ensures the user exists in the tenant and has role CLIENT. */
    public void requireClientInTenant(Long clientId, Long tenantId) {
        userRepository.findById(clientId)
                .filter(user -> tenantId.equals(user.getTenantId()))
                .filter(user -> user.getRole() == Role.CLIENT)
                .orElseThrow(() -> new BusinessException("Client not found in current tenant"));
    }
}
