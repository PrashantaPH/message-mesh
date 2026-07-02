package com.message.mesh.dto;

import com.message.mesh.enums.MessageStatus;

import java.util.UUID;

public record AckDto(UUID messageId, MessageStatus status) {
}
