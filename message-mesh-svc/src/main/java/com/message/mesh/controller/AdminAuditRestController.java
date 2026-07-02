package com.message.mesh.controller;

import com.message.mesh.dto.AuditEventDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Read-only access to the audit trail. Requires the {@code ADMIN} role.
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminAuditRestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditService auditService;

    @Operation(
            summary = "List audit events",
            description = "Returns a page of audit-trail entries, newest first. Optional 'actor' and 'action' "
                    + "filters match case-insensitively; optional 'from'/'to' (ISO-8601 instants) bound the "
                    + "created-at range; 'page' (0-based) and 'size' (max 100) paginate."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit events returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public PagedResponse<AuditEventDto> list(
            @RequestParam(name = "actor", required = false) String actor,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        return auditService.list(actor, action, parseInstant(from), parseInstant(to), pageable);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
