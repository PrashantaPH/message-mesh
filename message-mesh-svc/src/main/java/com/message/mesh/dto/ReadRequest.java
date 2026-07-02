package com.message.mesh.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record ReadRequest(
        @PositiveOrZero long seq
) {
}
