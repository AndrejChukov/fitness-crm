package ru.fitnesscrm.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.fitnesscrm.common.tenant.TenantContext;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtService.isValid(token)) {
                    var claims = jwtService.parseToken(token);
                    String email = claims.get("email", String.class);
                    UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    Long tenantId = parseTenantId(claims.get("tenantId"));
                    TenantContext.set(tenantId, principal.getId(), principal.getRole());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long parseTenantId(Object tenantClaim) {
        if (tenantClaim == null || tenantClaim.toString().isBlank()) {
            return null;
        }
        return Long.valueOf(tenantClaim.toString());
    }
}
