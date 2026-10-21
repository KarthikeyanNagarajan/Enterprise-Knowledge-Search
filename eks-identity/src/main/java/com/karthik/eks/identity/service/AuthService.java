package com.karthik.eks.identity.service;

import com.karthik.eks.identity.entity.User;
import com.karthik.eks.identity.repository.UserRepository;
import com.karthik.eks.identity.security.JwtTokenProvider;
import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service.
 * 
 * Responsibilities:
 * - User registration with password hashing
 * - User login with credential validation
 * - JWT token generation
 * - Password management
 * 
 * This service does NOT:
 * - Know about Documents or RAG
 * - Implement business logic beyond authentication
 * - Call any other module's services
 * 
 * Architecture: This service is a boundary - it processes only auth concerns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user.
     * 
     * Process:
     * 1. Validate that username and email don't already exist
     * 2. Hash the password using BCrypt
     * 3. Save the user entity
     * 4. Generate and return JWT token
     * 
     * @param username the username
     * @param email the user's email
     * @param password the plain text password (will be hashed)
     * @param firstName user's first name
     * @param lastName user's last name
     * @return LoginResponse with JWT token
     * @throws BusinessException if username or email already exists
     */
    @Transactional
    public LoginResponse register(String username, String email, String password, 
                                  String firstName, String lastName) {
        log.info("Registering new user: {}", username);

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already exists", HttpStatus.BAD_REQUEST);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        // Create new user with hashed password
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))  // BCrypt hashing
                .firstName(firstName)
                .lastName(lastName)
                .role(User.UserRole.USER)  // Default role for new users
                .isActive(true)
                .build();

        // Set audited fields
        user.setCreatedBy("SYSTEM");
        user.setUpdatedBy("SYSTEM");

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", username);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(savedUser);

        return LoginResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().toString())
                .token(token)
                .build();
    }

    /**
     * Authenticate user with username and password.
     * 
     * Process:
     * 1. Find user by username
     * 2. Validate password using BCrypt
     * 3. Check if user account is active
     * 4. Generate and return JWT token
     * 
     * @param username the username
     * @param password the plain text password
     * @return LoginResponse with JWT token
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException if password invalid or account inactive
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        log.info("User login attempt: {}", username);

        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", username);
            throw new BusinessException("Invalid password", HttpStatus.UNAUTHORIZED);
        }

        // Check if account is active
        if (!user.getIsActive()) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }

        log.info("User login successful: {}", username);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().toString())
                .token(token)
                .build();
    }

    /**
     * Get user by ID.
     * 
     * @param userId the user ID
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Validate JWT token and get user.
     * 
     * @param token the JWT token
     * @return User entity associated with token
     * @throws ResourceNotFoundException if token invalid or user not found
     */
    @Transactional(readOnly = true)
    public User validateTokenAndGetUser(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResourceNotFoundException("Invalid or expired token");
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        return getUserById(userId);
    }

    /**
     * Response DTO for login/register endpoints.
     */
    @lombok.Data
    @lombok.Builder
    public static class LoginResponse {
        private Long userId;
        private String username;
        private String email;
        private String role;
        private String token;
    }
}
