package com.message.mesh.controller;

import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.ChangePasswordRequest;
import com.message.mesh.dto.UpdateProfileRequest;
import com.message.mesh.dto.UserDto;
import com.message.mesh.exception.ApiError;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.service.PresenceService;
import com.message.mesh.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserRestController {

    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final ProfileService profileService;

    @Operation(
            summary = "Get the authenticated user's profile",
            description = "Returns the profile of the user resolved from the JWT in the request. "
                    + "Typically called once after login to hydrate the client session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Authenticated user no longer exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/me")
    public UserDto me(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Operation(
            summary = "Update the authenticated user's profile",
            description = "Updates the caller's own display name. Username and role cannot be changed here."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/me")
    public UserDto updateMe(@Valid @RequestBody UpdateProfileRequest request, Principal principal) {
        return profileService.updateProfile(principal.getName(), request.displayName());
    }

    @Operation(
            summary = "Change the authenticated user's password",
            description = "Verifies the current password, applies the new one, and returns a freshly issued "
                    + "JWT so the caller stays signed in on this device while other sessions are invalidated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed; new token returned"),
            @ApiResponse(responseCode = "400", description = "Current password incorrect or validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/me/password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest request, Principal principal) {
        return profileService.changePassword(principal.getName(),
                request.currentPassword(), request.newPassword());
    }

    @Operation(
            summary = "Delete the authenticated user's account",
            description = "Permanently deletes the caller's own account and memberships. Blocked when the "
                    + "caller is the last remaining administrator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "400", description = "Last administrator cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Principal principal) {
        profileService.deleteAccount(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List other users",
            description = "Returns every registered user except the caller. Intended to populate the "
                    + "member picker when starting a new conversation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<UserDto> list(Principal principal) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(principal.getName()))
                .map(UserDto::from)
                .toList();
    }

    @Operation(
            summary = "List online usernames",
            description = "Returns the set of usernames currently connected via WebSocket, derived from the "
                    + "in-memory presence registry. Use it to render online indicators in real time."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Online roster returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(type = "string")))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/online")
    public Set<String> online() {
        return presenceService.onlineUsernames();
    }
}
