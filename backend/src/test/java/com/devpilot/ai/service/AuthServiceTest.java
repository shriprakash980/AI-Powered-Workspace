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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 900000);
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterNewUser() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Alex Rivers")
                .email("alex@devpilot.ai")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("alex@devpilot.ai")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role("ROLE_USER")));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd-alex");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("Alex Rivers", response.getFullName());
        assertEquals("alex@devpilot.ai", response.getEmail());
        assertTrue(response.getRoles().contains("USER"));
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when email already exists")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Alex Rivers")
                .email("existing@devpilot.ai")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("existing@devpilot.ai")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    void shouldLoginWithValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("john@devpilot.ai")
                .password("Secret123!")
                .build();

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fullName("John Doe")
                .email("john@devpilot.ai")
                .passwordHash("hashed-secret")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(new Role("ROLE_USER")))
                .build();

        when(userRepository.findByEmail("john@devpilot.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashed-secret")).thenReturn(true);
        when(refreshTokenService.createRefreshToken(userId)).thenReturn("mocked-refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900, response.getExpiresIn());
        assertEquals("mocked-refresh-token", response.getRefreshToken());
        assertEquals("John Doe", response.getUser().getFullName());
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    void shouldRejectWrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("john@devpilot.ai")
                .password("WrongPassword")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("john@devpilot.ai")
                .passwordHash("hashed-secret")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("john@devpilot.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-secret")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should reject login when account is suspended")
    void shouldRejectSuspendedAccount() {
        LoginRequest request = LoginRequest.builder()
                .email("suspended@devpilot.ai")
                .password("Secret123!")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("suspended@devpilot.ai")
                .passwordHash("hashed-secret")
                .status(UserStatus.SUSPENDED)
                .build();

        when(userRepository.findByEmail("suspended@devpilot.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashed-secret")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should reject login when account is inactive")
    void shouldRejectInactiveAccount() {
        LoginRequest request = LoginRequest.builder()
                .email("inactive@devpilot.ai")
                .password("Secret123!")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("inactive@devpilot.ai")
                .passwordHash("hashed-secret")
                .status(UserStatus.INACTIVE)
                .build();

        when(userRepository.findByEmail("inactive@devpilot.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashed-secret")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should get current user profile")
    void shouldGetCurrentUser() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "John Doe", "john@devpilot.ai", "hash", UserStatus.ACTIVE, Set.of());

        User user = User.builder()
                .id(userId)
                .fullName("John Doe")
                .email("john@devpilot.ai")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(new Role("ROLE_USER")))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser(principal);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("John Doe", response.getFullName());
    }
}
