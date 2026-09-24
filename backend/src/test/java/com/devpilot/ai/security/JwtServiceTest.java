package com.devpilot.ai.security;

import com.devpilot.ai.entity.Role;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.entity.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 900000; // 15 mins

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid JWT access token and extract claims accurately")
    void shouldGenerateAndExtractClaims() {
        UUID userId = UUID.randomUUID();
        String email = "developer@devpilot.ai";
        List<String> roles = List.of("USER", "ADMIN");

        String token = jwtService.generateAccessToken(userId, email, roles);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(roles, jwtService.extractRoles(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should generate token from User entity")
    void shouldGenerateTokenFromUserEntity() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("jane@devpilot.ai")
                .fullName("Jane Doe")
                .roles(Set.of(new Role("ROLE_USER")))
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("jane@devpilot.ai", jwtService.extractEmail(token));
        assertTrue(jwtService.extractRoles(token).contains("USER"));
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens")
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Fast-expiring service with 1ms expiration
        JwtService shortLivedService = new JwtService(SECRET, 1);
        String token = shortLivedService.generateAccessToken(UUID.randomUUID(), "exp@test.com", List.of("USER"));

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}

        assertFalse(shortLivedService.validateToken(token));
        assertTrue(shortLivedService.isTokenExpired(token));
    }
}
