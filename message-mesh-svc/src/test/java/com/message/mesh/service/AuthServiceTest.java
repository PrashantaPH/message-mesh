package com.message.mesh.service;

import com.message.mesh.domain.User;
import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.LoginRequest;
import com.message.mesh.dto.RegisterRequest;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register hashes the password, persists the user and returns a signed token")
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest("alice", "secret123", "Alice");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(jwtUtil.generateToken("alice", 0)).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("alice");
        assertThat(response.user().displayName()).isEqualTo("Alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("register rejects a username that is already taken")
    void registerRejectsDuplicateUsername() {
        RegisterRequest req = new RegisterRequest("alice", "secret123", "Alice");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login authenticates the credentials and returns a token for the user")
    void loginReturnsTokenForValidCredentials() {
        LoginRequest req = new LoginRequest("alice", "secret123");
        User user = User.builder()
                .username("alice").passwordHash("hashed").displayName("Alice")
                .role(UserRole.USER).tokenVersion(2).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("alice", 2)).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("alice");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("alice", "secret123"));
    }

    @Test
    @DisplayName("login propagates authentication failures")
    void loginPropagatesAuthenticationFailure() {
        LoginRequest req = new LoginRequest("alice", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("login throws when the authenticated user cannot be found")
    void loginThrowsWhenUserMissing() {
        LoginRequest req = new LoginRequest("ghost", "secret123");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
