package com.example.activity.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-that-is-longer-than-thirty-two-bytes",
            java.time.Duration.ofMinutes(10)
    );

    @Test
    void generatesAndValidatesToken() {
        UserDetails user = User.withUsername("student")
                .password("ignored")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(user);

        assertEquals("student", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
        assertEquals(600, jwtService.getExpirationSeconds());
    }
}
