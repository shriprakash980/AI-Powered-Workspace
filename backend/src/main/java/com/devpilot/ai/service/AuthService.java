package com.devpilot.ai.service;

import com.devpilot.ai.dto.user.*;
import com.devpilot.ai.entity.Role;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.UnauthorizedException;
import com.devpilot.ai.repository.RoleRepository;
import com.devpilot.ai.repository.UserRepository;
import com.devpilot.ai.security.JwtService;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty()) {
            throw new BadRequestException("Email is required");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: email '{}' already in use", normalizedEmail);
            throw new BadRequestException("An account with this email address already exists");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        Instant now = Instant.now();
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with email '{}' and ID: {}", normalizedEmail, savedUser.getId());
        return mapToUserResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty() || request.getPassword() == null) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: no user with email '{}'", normalizedEmail);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for email '{}'", normalizedEmail);
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login rejected: account suspended for email '{}'", normalizedEmail);
            throw new UnauthorizedException("User account has been suspended");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login rejected: account inactive for email '{}'", normalizedEmail);
            throw new UnauthorizedException("User account is inactive");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("Successful login for email '{}' (User ID: {})", normalizedEmail, user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .refreshToken(rawRefreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenService.RefreshTokenRotationResult result =
                refreshTokenService.verifyAndRotateRefreshToken(request.getRefreshToken());

        User user = result.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = result.getNewRefreshToken();

        log.info("Refreshed tokens for user ID: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .refreshToken(newRefreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public void logout(LogoutRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return mapToUserResponse(user);
    }

    public UserResponse mapToUserResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roleNames)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
