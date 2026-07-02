package com.message.mesh.controller;

import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.LoginRequest;
import com.message.mesh.dto.RegisterRequest;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
@SecurityRequirements // public endpoints: no bearer token required
public class AuthRestController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new account",
            description = "Creates a user with a unique username and returns a signed JWT together with "
                    + "the created profile. The returned token authorizes all subsequent REST calls and "
                    + "the WebSocket handshake. No authentication is required to call this endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created; JWT issued"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. missing or too-short fields)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username already taken",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(
            summary = "Authenticate and obtain a JWT",
            description = "Validates the supplied credentials and, on success, returns a signed JWT and the "
                    + "user profile. Use the token as 'Authorization: Bearer <token>' for protected endpoints."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials accepted; JWT issued"),
            @ApiResponse(responseCode = "400", description = "Missing username or password",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
