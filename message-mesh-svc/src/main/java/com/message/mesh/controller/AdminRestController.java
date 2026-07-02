package com.message.mesh.controller;

import com.message.mesh.dto.AdminUserDetailDto;
import com.message.mesh.dto.AdminUserDto;
import com.message.mesh.dto.CreateUserRequest;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.dto.ResetPasswordRequest;
import com.message.mesh.dto.UpdateRoleRequest;
import com.message.mesh.dto.UpdateStatusRequest;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * Administrative user-management endpoints. Every operation requires the
 * {@code ADMIN} role, enforced server-side via {@link PreAuthorize}.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminRestController {

    private final AdminService adminService;

    private static final int MAX_PAGE_SIZE = 100;

    @Operation(
            summary = "List all users",
            description = "Returns a page of registered users with administrative details (role, active state, "
                    + "online status, conversation count). 'q' filters by username or display name; "
                    + "'page' (0-based) and 'size' (max 100) control pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public PagedResponse<AdminUserDto> list(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "role", required = false) UserRole role,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("username").ascending());
        return adminService.listUsers(query, role, active, pageable);
    }

    @Operation(
            summary = "Create a user",
            description = "Provisions a new account with the given username, display name, password and role. "
                    + "The username must be unique; the password is BCrypt-hashed and never returned."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation error or username already taken",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<AdminUserDto> create(@Valid @RequestBody CreateUserRequest request,
                                               Principal principal) {
        AdminUserDto created = adminService.createUser(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Get user detail",
            description = "Returns a single user's administrative record together with the conversations they "
                    + "belong to (admin drill-down view)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User detail returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public AdminUserDetailDto detail(@PathVariable UUID id) {
        return adminService.getUserDetail(id);
    }

    @Operation(
            summary = "Change a user's role",
            description = "Promotes a user to ADMIN or demotes to USER. The last remaining administrator "
                    + "cannot be demoted, and admins cannot revoke their own role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Guardrail violation",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/role")
    public AdminUserDto updateRole(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateRoleRequest request,
                                   Principal principal) {
        return adminService.updateRole(id, request.role(), principal.getName());
    }

    @Operation(
            summary = "Activate or deactivate a user",
            description = "Deactivated users cannot log in and their existing sessions are rejected. The last "
                    + "active administrator cannot be deactivated, and admins cannot deactivate themselves."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Guardrail violation",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/status")
    public AdminUserDto updateStatus(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateStatusRequest request,
                                     Principal principal) {
        return adminService.updateStatus(id, request.active(), principal.getName());
    }

    @Operation(
            summary = "Reset a user's password",
            description = "Sets a new password for the target user. The value is BCrypt-hashed and never returned."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset"),
            @ApiResponse(responseCode = "400", description = "Invalid password",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                              @Valid @RequestBody ResetPasswordRequest request,
                                              Principal principal) {
        adminService.resetPassword(id, request.newPassword(), principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a user",
            description = "Permanently removes a user and their conversation memberships. Admins cannot delete "
                    + "themselves or the last remaining administrator. Authored messages are retained."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "400", description = "Guardrail violation",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        adminService.deleteUser(id, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Revoke a user's sessions (force logout)",
            description = "Invalidates all of the user's outstanding JWTs by advancing their token version, "
                    + "forcing them to sign in again on every device."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessions revoked"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/revoke-sessions")
    public AdminUserDto revokeSessions(@PathVariable UUID id, Principal principal) {
        return adminService.revokeSessions(id, principal.getName());
    }
}
