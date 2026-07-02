package com.message.mesh.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AckRequest(@NotNull UUID messageId) {
}
