package com.message.mesh.controller;

import com.message.mesh.dto.EditMessageRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.ReactionRequest;
import com.message.mesh.exception.ApiError;
import com.message.mesh.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Messages")
public class MessageRestController {

    private final MessageService messageService;

    @Operation(
            summary = "Edit a message",
            description = "Updates the body of a message. Only the original author may edit, and deleted "
                    + "messages cannot be edited. The updated message is re-broadcast to the conversation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message updated"),
            @ApiResponse(responseCode = "400", description = "Not the author, message deleted, or invalid body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Message not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}")
    public MessageDto edit(
            @Parameter(description = "Message identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody EditMessageRequest req,
            Principal principal) {
        return messageService.editMessage(principal.getName(), id, req.body());
    }

    @Operation(
            summary = "Delete a message",
            description = "Soft-deletes a message (its body is cleared and it renders as a tombstone). The "
                    + "author or a group admin may delete. The tombstone is re-broadcast to the conversation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message deleted (tombstone returned)"),
            @ApiResponse(responseCode = "400", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Message not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public MessageDto delete(
            @Parameter(description = "Message identifier", required = true)
            @PathVariable UUID id,
            Principal principal) {
        return messageService.deleteMessage(principal.getName(), id);
    }

    @Operation(
            summary = "Add a reaction",
            description = "Adds an emoji reaction from the caller (from a fixed palette). Idempotent per "
                    + "user+emoji. The updated message is re-broadcast to the conversation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reaction added"),
            @ApiResponse(responseCode = "400", description = "Unsupported emoji or not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Message not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/reactions")
    public MessageDto addReaction(
            @Parameter(description = "Message identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ReactionRequest req,
            Principal principal) {
        return messageService.addReaction(principal.getName(), id, req.emoji());
    }

    @Operation(
            summary = "Remove a reaction",
            description = "Removes the caller's emoji reaction. The updated message is re-broadcast to the "
                    + "conversation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reaction removed"),
            @ApiResponse(responseCode = "400", description = "Not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Message not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}/reactions/{emoji}")
    public MessageDto removeReaction(
            @Parameter(description = "Message identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "URL-encoded emoji to remove", required = true)
            @PathVariable String emoji,
            Principal principal) {
        return messageService.removeReaction(principal.getName(), id, emoji);
    }
}
