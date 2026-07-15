package ru.fitnesscrm.identity.service;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.identity.api.dto.response.AuthResponse;
import ru.fitnesscrm.identity.api.dto.request.LoginRequest;
import ru.fitnesscrm.identity.api.dto.request.RegisterTenantRequest;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.identity.security.JwtService;
import ru.fitnesscrm.identity.security.UserPrincipal;

@Service
@AllArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest request) {
        if (tenantRepository.existsBySlug(request.tenantSlug())) {
            throw new BusinessException("Tenant slug already exists: " + request.tenantSlug());
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName());
        tenant.setSlug(request.tenantSlug());
        tenant.setActive(true);
        tenantRepository.save(tenant);

        User admin = new User();
        admin.setTenant(tenant);
        admin.setEmail(request.email().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.TENANT_ADMIN);
        admin.setFirstName(request.firstName());
        admin.setLastName(request.lastName());
        admin.setActive(true);
        userRepository.save(admin);

        UserPrincipal principal = UserPrincipal.from(admin);
        return buildAuthResponse(principal);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase(),
                        request.password()
                )
        );
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BusinessException("User not found"));
        return buildAuthResponse(UserPrincipal.from(user));
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        String token = jwtService.generateToken(principal);
        return new AuthResponse(
                token,
                "Bearer",
                principal.getId(),
                principal.getTenantId(),
                principal.getUsername(),
                principal.getRole().name()
        );
    }
}
