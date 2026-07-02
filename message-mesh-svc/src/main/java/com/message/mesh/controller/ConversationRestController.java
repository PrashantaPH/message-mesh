package com.message.mesh.controller;

import com.message.mesh.dto.AddMembersRequest;
import com.message.mesh.dto.ConversationDto;
import com.message.mesh.dto.ConversationMemberDto;
import com.message.mesh.dto.CreateConversationRequest;
import com.message.mesh.dto.MembershipPrefsRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.dto.ReadRequest;
import com.message.mesh.dto.RenameConversationRequest;
import com.message.mesh.dto.UpdateMemberRoleRequest;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations")
public class ConversationRestController {

    private final ConversationService conversationService;

    @Operation(
            summary = "List the caller's conversations",
            description = "Returns every conversation the authenticated user is a member of, each enriched "
                    + "with its most recent message and the caller's unread count. Useful for rendering the "
                    + "conversation sidebar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversations returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<ConversationDto> list(Principal principal) {
        return conversationService.listForUser(principal.getName());
    }

    @Operation(
            summary = "Create a conversation",
            description = "Creates a DIRECT (exactly two members) or GROUP conversation. The caller is added "
                    + "automatically; 'memberUsernames' lists the other participants. For DIRECT chats an "
                    + "existing conversation with the same pair may be reused."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conversation created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload (e.g. empty members, bad type)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "One or more member usernames do not exist",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ConversationDto> create(@Valid @RequestBody CreateConversationRequest req,
                                                  Principal principal) {
        ConversationDto created = conversationService.create(principal.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Fetch message history",
            description = "Returns messages for a conversation ordered by ascending sequence number. Use "
                    + "'afterSeq' for forward, cursor-style pagination: pass the highest 'seq' you already "
                    + "hold to fetch only newer messages, and bound the page with 'limit'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of the conversation",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/messages")
    public List<MessageDto> messages(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Return only messages with a sequence greater than this value (cursor)",
                    example = "0")
            @RequestParam(defaultValue = "0") long afterSeq,
            @Parameter(description = "Maximum number of messages to return", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            Principal principal) {
        return conversationService.getMessages(principal.getName(), id, afterSeq, limit);
    }

    @Operation(
            summary = "Mark a conversation as read",
            description = "Advances the caller's read marker to the supplied sequence number, clearing the "
                    + "unread count up to and including that message and triggering read receipts to other "
                    + "members. Returns 204 No Content on success."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Read marker updated"),
            @ApiResponse(responseCode = "400", description = "Sequence must be zero or positive",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of the conversation",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ReadRequest req,
            Principal principal) {
        conversationService.markRead(principal.getName(), id, req.seq());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List a conversation's members",
            description = "Returns every participant of the conversation together with their membership role "
                    + "(ADMIN or MEMBER). Useful for rendering a group roster showing the member count and who "
                    + "the admins are. Admins are returned first."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of the conversation",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/members")
    public List<ConversationMemberDto> members(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            Principal principal) {
        return conversationService.getMembers(principal.getName(), id);
    }

    @Operation(
            summary = "Search messages within a conversation",
            description = "Case-insensitive search over the conversation's non-deleted messages, newest first, "
                    + "with page-based pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching messages returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a member of the conversation",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/messages/search")
    public PagedResponse<MessageDto> searchMessages(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Search text", example = "hello")
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return conversationService.searchMessages(principal.getName(), id, q, page, size);
    }

    @Operation(
            summary = "Rename a group conversation",
            description = "Updates the title of a GROUP conversation. Only group admins may rename."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation renamed"),
            @ApiResponse(responseCode = "400", description = "Not a group, not an admin, or invalid title",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}")
    public ConversationDto rename(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody RenameConversationRequest req,
            Principal principal) {
        return conversationService.renameGroup(principal.getName(), id, req.title());
    }

    @Operation(
            summary = "Add members to a group",
            description = "Adds one or more users (by username) to a GROUP conversation. Only group admins may add."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members added; updated roster returned"),
            @ApiResponse(responseCode = "400", description = "Not a group or caller is not an admin",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "A username does not exist",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/members")
    public List<ConversationMemberDto> addMembers(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody AddMembersRequest req,
            Principal principal) {
        return conversationService.addMembers(principal.getName(), id, req.usernames());
    }

    @Operation(
            summary = "Remove a member (or leave)",
            description = "Removes a member from a group. Admins may remove anyone; a non-admin may remove only "
                    + "themselves (leave). The last admin cannot be removed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed"),
            @ApiResponse(responseCode = "400", description = "Not permitted or would remove the last admin",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Target user identifier", required = true)
            @PathVariable UUID userId,
            Principal principal) {
        conversationService.removeMember(principal.getName(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Change a member's role",
            description = "Promotes or demotes a group member. Only group admins may change roles; the last "
                    + "admin cannot be demoted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated; updated roster returned"),
            @ApiResponse(responseCode = "400", description = "Not permitted or would demote the last admin",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/members/{userId}/role")
    public List<ConversationMemberDto> updateMemberRole(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Target user identifier", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest req,
            Principal principal) {
        return conversationService.updateMemberRole(principal.getName(), id, userId, req.role());
    }

    @Operation(
            summary = "Update per-user conversation preferences",
            description = "Toggles the caller's own mute and/or archive flags for the conversation. Omitted "
                    + "fields are left unchanged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences updated"),
            @ApiResponse(responseCode = "400", description = "Caller is not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/membership")
    public ConversationDto updateMembership(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            @RequestBody MembershipPrefsRequest req,
            Principal principal) {
        return conversationService.setMembershipPrefs(principal.getName(), id, req);
    }

    @Operation(
            summary = "Delete or leave a conversation",
            description = "A group admin soft-deletes the whole conversation for everyone; any other member "
                    + "simply leaves (their membership is removed)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conversation deleted or left"),
            @ApiResponse(responseCode = "400", description = "Caller is not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Conversation identifier", required = true)
            @PathVariable UUID id,
            Principal principal) {
        conversationService.deleteConversation(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
