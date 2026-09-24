package com.devpilot.ai.repository;

import com.devpilot.ai.entity.Role;
import com.devpilot.ai.entity.User;
import com.devpilot.ai.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldPersistAndRetrieveUserWithRoles() {
        Role role = roleRepository.save(new Role("ROLE_DEVELOPER"));

        User user = User.builder()
                .fullName("Alex Turing")
                .email("alex@devpilot.ai")
                .passwordHash("$2a$12$securePasswordHashExampleHere")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());

        Optional<User> found = userRepository.findByEmail("alex@devpilot.ai");
        assertTrue(found.isPresent());
        assertEquals("Alex Turing", found.get().getFullName());
        assertEquals(1, found.get().getRoles().size());
    }

    @Test
    void shouldThrowExceptionOnDuplicateEmail() {
        User user1 = User.builder()
                .fullName("User One")
                .email("duplicate@devpilot.ai")
                .passwordHash("hash1")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .fullName("User Two")
                .email("duplicate@devpilot.ai")
                .passwordHash("hash2")
                .status(UserStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}
