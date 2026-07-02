package com.message.mesh.controller;

import com.message.mesh.dto.AdminStatsDto;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aggregate platform metrics for the admin dashboard. Requires the {@code ADMIN} role.
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminStatsRestController {

    private final AdminService adminService;

    @Operation(
            summary = "Get platform statistics",
            description = "Returns aggregate counts for users, conversations, messages and audit activity "
                    + "used to populate the admin dashboard."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public AdminStatsDto stats() {
        return adminService.stats();
    }
}
