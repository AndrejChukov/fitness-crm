package ru.fitnesscrm.identity.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.User;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    @Getter
    private final Long id;
    @Getter
    private final Long tenantId;
    private final String email;
    private final String passwordHash;
    @Getter
    private final Role role;
    private final boolean active;

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
