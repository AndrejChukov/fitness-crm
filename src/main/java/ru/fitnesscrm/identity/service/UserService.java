package ru.fitnesscrm.identity.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.identity.api.dto.request.CreateUserRequest;
import ru.fitnesscrm.identity.api.dto.response.UserResponse;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAllForCurrentTenant() {
        Long tenantId = requireTenantId();
        return userRepository.findByTenant_Id(tenantId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        assertSameTenant(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        Long tenantId = requireTenantId();
        if (request.role() == Role.SUPER_ADMIN) {
            throw new BusinessException("Cannot create SUPER_ADMIN via tenant API");
        }
        if (userRepository.existsByTenant_IdAndEmail(tenantId, request.email().toLowerCase())) {
            throw new BusinessException("Email already registered in this tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setActive(true);
        return UserResponse.from(userRepository.save(user));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        return tenantId;
    }

    private void assertSameTenant(User user) {
        if (!requireTenantId().equals(user.getTenantId())) {
            throw new ResourceNotFoundException("User not found");
        }
    }
}
