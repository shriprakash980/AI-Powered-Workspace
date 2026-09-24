package com.devpilot.ai.security;

import com.devpilot.ai.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${app.security.jwt-secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${app.security.jwt-access-token-expiration:900000}") long accessTokenExpirationMs) {
        this.key = parseSecretKey(secret);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    private SecretKey parseSecretKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length >= 32) {
                return Keys.hmacShaKeyFor(keyBytes);
            }
        } catch (Exception ignored) {
            // Fall back to UTF-8 bytes
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(UserPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return buildToken(principal.getId(), principal.getEmail(), roles);
    }

    public String generateAccessToken(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return buildToken(user.getId(), user.getEmail(), roles);
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        return buildToken(userId, email, roles);
    }

    private String buildToken(UUID userId, String email, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer("devpilot-ai")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token");
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or null");
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        String subject = extractAllClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return Collections.emptyList();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
