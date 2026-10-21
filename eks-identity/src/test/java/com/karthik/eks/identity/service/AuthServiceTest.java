package com.karthik.eks.identity.service;

import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import com.karthik.eks.identity.entity.User;
import com.karthik.eks.identity.repository.UserRepository;
import com.karthik.eks.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        lenient().when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");
    }

    @Test
    void registerShouldCreateUserAndReturnToken() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);

        User savedUser = User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("encoded-password")
                .firstName("Alice")
                .lastName("User")
                .role(User.UserRole.USER)
                .isActive(true)
                .build();
        savedUser.setId(1L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthService.LoginResponse response = authService.register(
                "alice",
                "alice@example.com",
                "Password123!",
                "Alice",
                "User"
        );

        assertNotNull(response);
        assertEquals("alice", response.getUsername());
        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldFailWhenUsernameExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register("alice", "alice@example.com", "Password123!", "Alice", "User"));

        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void registerShouldFailWhenEmailExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register("alice", "alice@example.com", "Password123!", "Alice", "User"));

        assertEquals("Email already exists", ex.getMessage());
    }

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() {
        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("encoded-password")
                .firstName("Alice")
                .lastName("User")
                .role(User.UserRole.USER)
                .isActive(true)
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");

        AuthService.LoginResponse response = authService.login("alice", "Password123!");

        assertEquals("alice", response.getUsername());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void loginShouldFailWhenPasswordIsInvalid() {
        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("encoded-password")
                .firstName("Alice")
                .lastName("User")
                .role(User.UserRole.USER)
                .isActive(true)
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "encoded-password")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("alice", "WrongPass"));

        assertEquals("Invalid password", ex.getMessage());
    }

    @Test
    void loginShouldFailWhenUserMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.login("missing", "Password123!"));

        assertTrue(ex.getMessage().contains("User not found"));
    }
}
