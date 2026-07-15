package ru.fitnesscrm.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fitnesscrm.config.JwtProperties;
import ru.fitnesscrm.identity.domain.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
@AllArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public String generateToken(UserPrincipal principal) {
        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claims(Map.of(
                        "email", principal.getUsername(),
                        "role", principal.getRole().name(),
                        "tenantId", principal.getTenantId() != null ? principal.getTenantId() : ""
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.expirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
