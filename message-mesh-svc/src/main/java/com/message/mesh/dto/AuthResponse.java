package com.message.mesh.dto;

public record AuthResponse(
        String token,
        UserDto user
) {
}
