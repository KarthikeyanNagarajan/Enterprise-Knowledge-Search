package com.karthik.eks.identity.security;

import com.karthik.eks.identity.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateAndValidateToken() throws Exception {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();

        var secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtTokenProvider, "MyVerySecureSecretKeyForJWTTokenGenerationAndValidation123456789");

        var expField = JwtTokenProvider.class.getDeclaredField("jwtExpirationMs");
        expField.setAccessible(true);
        expField.setLong(jwtTokenProvider, 86400000L);

        User user = User.builder()
                .username("alice")
                .firstName("Alice")
                .lastName("User")
                .role(User.UserRole.USER)
                .build();
        user.setId(42L);

        String token = jwtTokenProvider.generateToken(user);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("alice", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(42L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("USER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }
}
