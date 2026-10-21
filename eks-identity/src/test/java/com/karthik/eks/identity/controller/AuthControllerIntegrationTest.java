package com.karthik.eks.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karthik.eks.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void registerEndpointShouldCreateUser() throws Exception {
        String username = "controller_user_" + System.currentTimeMillis();
        String email = "controller_" + System.currentTimeMillis() + "@example.com";

        when(authService.register(any(), any(), any(), any(), any()))
                .thenReturn(AuthService.LoginResponse.builder()
                        .userId(1L)
                        .username(username)
                        .email(email)
                        .role("USER")
                        .token("mock-jwt-token")
                        .build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Controller",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    void loginEndpointShouldReturnJwtToken() throws Exception {
        String username = "login_user_" + System.currentTimeMillis();
        String email = "login_" + System.currentTimeMillis() + "@example.com";

        when(authService.login(any(), any()))
                .thenReturn(AuthService.LoginResponse.builder()
                        .userId(2L)
                        .username(username)
                        .email(email)
                        .role("USER")
                        .token("mock-login-token")
                        .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-login-token"));
    }
}
