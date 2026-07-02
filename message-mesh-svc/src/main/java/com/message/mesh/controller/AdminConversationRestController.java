package com.message.mesh.controller;

import com.message.mesh.dto.AdminConversationDto;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.AdminConversationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * Administrative conversation oversight endpoints. Requires the {@code ADMIN} role.
 */
@RestController
@RequestMapping("/api/admin/conversations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminConversationRestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminConversationService adminConversationService;

    @Operation(
            summary = "List all conversations",
            description = "Returns a page of conversations with member and message counts. 'q' filters by "
                    + "title; 'page' (0-based) and 'size' (max 100) control pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversations returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public PagedResponse<AdminConversationDto> list(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "type", required = false) ConversationType type,
            @RequestParam(name = "deleted", required = false) Boolean deleted,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        return adminConversationService.listConversations(query, type, deleted, pageable);
    }

    @Operation(
            summary = "Soft-delete a conversation",
            description = "Marks a conversation as deleted so it disappears from members' lists. The record "
                    + "and its history are preserved."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation soft-deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public AdminConversationDto delete(@PathVariable UUID id, Principal principal) {
        return adminConversationService.softDelete(id, principal.getName());
    }

    @Operation(
            summary = "Restore a soft-deleted conversation",
            description = "Reverses a soft-delete so the conversation reappears for its members."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation restored"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/restore")
    public AdminConversationDto restore(@PathVariable UUID id, Principal principal) {
        return adminConversationService.restore(id, principal.getName());
    }

    @Operation(
            summary = "List messages in a conversation",
            description = "Returns a page of messages (newest first) for moderation review, including "
                    + "soft-deleted tombstones. 'page' (0-based) and 'size' (max 100) paginate."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/messages")
    public PagedResponse<MessageDto> messages(
            @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return adminConversationService.listMessages(id, pageable);
    }

    @Operation(
            summary = "Delete a message (moderation)",
            description = "Soft-deletes an individual message within a conversation and broadcasts the "
                    + "tombstone to connected members in real time."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Message or conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}/messages/{messageId}")
    public MessageDto deleteMessage(@PathVariable UUID id,
                                    @PathVariable UUID messageId,
                                    Principal principal) {
        return adminConversationService.deleteMessage(id, messageId, principal.getName());
    }
}
