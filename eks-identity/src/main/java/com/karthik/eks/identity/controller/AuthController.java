package com.karthik.eks.identity.controller;

import com.karthik.eks.identity.service.AuthService;
import com.karthik.eks.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller.
 * 
 * Endpoints:
 * - POST /auth/register: Register new user
 * - POST /auth/login: Login and get JWT token
 * 
 * Request/Response format:
 * - All requests/responses are wrapped in ApiResponse
 * - Sensitive data like passwords are not logged
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     * 
     * Request Body:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "secure_password",
     *   "firstName": "John",
     *   "lastName": "Doe"
     * }
     * 
     * Response (201 Created):
     * {
     *   "success": true,
     *   "message": "User registered successfully",
     *   "data": {
     *     "userId": 1,
     *     "username": "john_doe",
     *     "email": "john@example.com",
     *     "role": "USER",
     *     "token": "eyJhbGc..."
     *   }
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthService.LoginResponse>> register(
            @RequestBody RegisterRequest request) {
        
        log.info("Registration request for username: {}", request.getUsername());
        
        AuthService.LoginResponse response = authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * Login and get JWT token.
     * 
     * Request Body:
     * {
     *   "username": "john_doe",
     *   "password": "secure_password"
     * }
     * 
     * Response (200 OK):
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": {
     *     "userId": 1,
     *     "username": "john_doe",
     *     "email": "john@example.com",
     *     "role": "USER",
     *     "token": "eyJhbGc..."
     *   }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthService.LoginResponse>> login(
            @RequestBody LoginRequest request) {
        
        log.info("Login request for username: {}", request.getUsername());
        
        AuthService.LoginResponse response = authService.login(
                request.getUsername(),
                request.getPassword()
        );

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Request DTO for registration.
     */
    @lombok.Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    /**
     * Request DTO for login.
     */
    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
