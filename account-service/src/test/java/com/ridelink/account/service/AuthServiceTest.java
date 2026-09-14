package com.ridelink.account.service;

import com.ridelink.account.dto.AuthResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private static RegisterRequest passengerRequest() {
        return new RegisterRequest("Ayesha Perera", "Ayesha@Example.com", "Passw0rd!", "0771234567", Role.PASSENGER);
    }

    @Test
    void registerHashesPasswordAndNormalisesEmail() {
        when(users.existsByEmail("ayesha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(passengerRequest());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ayesha@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASHED");
        assertThat(response.role()).isEqualTo(Role.PASSENGER);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("ayesha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(passengerRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((ApiException) ex).getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });
        verify(users, never()).save(any());
    }

    @Test
    void registerRejectsAdminRole() {
        RegisterRequest req = new RegisterRequest("Root", "root@example.com", "Passw0rd!", null, Role.ADMIN);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ROLE_NOT_ALLOWED"));
        verifyNoInteractions(users);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User("Ayesha", "ayesha@example.com", "HASHED", null, Role.PASSENGER);
        when(users.findByEmail("ayesha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!", "HASHED")).thenReturn(true);
        Instant expiry = Instant.now().plusSeconds(3600);
        when(jwtService.expiryFromNow()).thenReturn(expiry);
        when(jwtService.generateToken(user, expiry)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ayesha@example.com", "Passw0rd!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresAt()).isEqualTo(expiry);
        assertThat(response.user().email()).isEqualTo("ayesha@example.com");
    }

    @Test
    void loginRejectsUnknownEmailWithGenericMessage() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "x")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_CREDENTIALS"));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("Ayesha", "ayesha@example.com", "HASHED", null, Role.PASSENGER);
        when(users.findByEmail("ayesha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ayesha@example.com", "wrong")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginRejectsSuspendedAccountAfterPasswordCheck() {
        User user = new User("Ayesha", "ayesha@example.com", "HASHED", null, Role.PASSENGER);
        user.setStatus(AccountStatus.SUSPENDED);
        when(users.findByEmail("ayesha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!", "HASHED")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ayesha@example.com", "Passw0rd!")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ACCOUNT_SUSPENDED"));
        verifyNoInteractions(jwtService);
    }
}