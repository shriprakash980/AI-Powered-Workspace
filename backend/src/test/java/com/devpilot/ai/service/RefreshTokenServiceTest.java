package com.devpilot.ai.service;

import com.devpilot.ai.entity.RefreshToken;
import com.devpilot.ai.entity.Role;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.UnauthorizedException;
import com.devpilot.ai.repository.RefreshTokenRepository;
import com.devpilot.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;
    private static final long EXPIRATION_MS = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should create raw refresh token and store SHA-256 hashed entity")
    void shouldCreateRefreshToken() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(userId);

        assertNotNull(rawToken);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertFalse(saved.isRevoked());
        assertEquals(RefreshTokenService.hashToken(rawToken), saved.getTokenHash());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should verify and rotate valid refresh token")
    void shouldVerifyAndRotateValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        String rawToken = "sample-raw-token-value-12345";
        String tokenHash = RefreshTokenService.hashToken(rawToken);

        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        User user = User.builder()
                .id(userId)
                .email("user@devpilot.ai")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(new Role("ROLE_USER")))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshTokenService.RefreshTokenRotationResult result =
                refreshTokenService.verifyAndRotateRefreshToken(rawToken);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertNotNull(result.getNewRefreshToken());
        assertNotEquals(rawToken, result.getNewRefreshToken());

        // Ensure old token was revoked
        assertTrue(existing.isRevoked());
    }

    @Test
    @DisplayName("Should reject revoked refresh token")
    void shouldRejectRevokedRefreshToken() {
        String rawToken = "revoked-raw-token";
        String tokenHash = RefreshTokenService.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThrows(UnauthorizedException.class, () ->
                refreshTokenService.verifyAndRotateRefreshToken(rawToken));
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() {
        String rawToken = "expired-raw-token";
        String tokenHash = RefreshTokenService.hashToken(rawToken);

        RefreshToken expiredToken = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(100))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThrows(UnauthorizedException.class, () ->
                refreshTokenService.verifyAndRotateRefreshToken(rawToken));
    }
}
