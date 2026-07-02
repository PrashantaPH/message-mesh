package com.message.mesh.controller;

import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.LoginRequest;
import com.message.mesh.dto.RegisterRequest;
import com.message.mesh.dto.UserDto;
import com.message.mesh.enums.UserRole;
import com.message.mesh.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRestController")
class AuthRestControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthRestController controller;

    private AuthResponse authResponse() {
        return new AuthResponse("jwt",
                new UserDto(UUID.randomUUID(), "alice", "Alice", UserRole.USER));
    }

    @Test
    @DisplayName("register responds 201 Created with the issued token")
    void registerReturnsCreated() {
        RegisterRequest req = new RegisterRequest("alice", "secret1", "Alice");
        AuthResponse expected = authResponse();
        when(authService.register(req)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = controller.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    @DisplayName("login responds 200 OK with the issued token")
    void loginReturnsOk() {
        LoginRequest req = new LoginRequest("alice", "secret1");
        AuthResponse expected = authResponse();
        when(authService.login(req)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = controller.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }
}
