package com.ridelink.account.security;

import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-characters-long";
    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60);
        user = new User("Ayesha Perera", "ayesha@example.com", "hash", "0771234567", Role.PASSENGER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    }

    @Test
    void generatedTokenContainsContractClaims() {
        String token = jwtService.generateToken(user, jwtService.expiryFromNow());
        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("ayesha@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("PASSENGER");
        assertThat(claims.get("status", String.class)).isEqualTo("ACTIVE");
        assertThat(claims.getExpiration()).isAfter(Instant.now());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateToken(user, Instant.now().minusSeconds(60));
        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-key-of-32-chars!", 60);
        String token = other.generateToken(user, other.expiryFromNow());
        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user, jwtService.expiryFromNow());
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRejectedAtConstruction() {
        assertThatThrownBy(() -> new JwtService("too-short", 60))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }
}