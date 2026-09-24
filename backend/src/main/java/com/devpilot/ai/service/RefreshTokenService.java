package com.devpilot.ai.service;

import com.devpilot.ai.entity.RefreshToken;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.UnauthorizedException;
import com.devpilot.ai.repository.RefreshTokenRepository;
import com.devpilot.ai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.security.jwt-refresh-token-expiration:604800000}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String createRefreshToken(UUID userId) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(token);
        log.info("Created new refresh token session for user ID: {}", userId);
        return rawToken;
    }

    public RefreshTokenRotationResult verifyAndRotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database");
                    return new UnauthorizedException("Invalid refresh token");
                });

        if (storedToken.isRevoked()) {
            log.warn("Attempt to use revoked refresh token for user ID: {}", storedToken.getUserId());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Attempt to use expired refresh token for user ID: {}", storedToken.getUserId());
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to refresh token for inactive/suspended user ID: {}", user.getId());
            throw new UnauthorizedException("User account is not active");
        }

        // Revoke the old token (token rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new refresh token
        String newRawToken = createRefreshToken(user.getId());

        log.info("Successfully rotated refresh token for user ID: {}", user.getId());
        return new RefreshTokenRotationResult(user, newRawToken);
    }

    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.trim().isEmpty()) {
            String tokenHash = hashToken(rawRefreshToken.trim());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token for user ID: {}", token.getUserId());
            });
        }
    }

    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Revoked all refresh tokens for user ID: {}", userId);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static class RefreshTokenRotationResult {
        private final User user;
        private final String newRefreshToken;

        public RefreshTokenRotationResult(User user, String newRefreshToken) {
            this.user = user;
            this.newRefreshToken = newRefreshToken;
        }

        public User getUser() {
            return user;
        }

        public String getNewRefreshToken() {
            return newRefreshToken;
        }
    }
}
