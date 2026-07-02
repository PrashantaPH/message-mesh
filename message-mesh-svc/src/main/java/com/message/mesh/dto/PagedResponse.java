package com.message.mesh.dto;

import java.util.List;

/**
 * Generic page envelope for list endpoints. Kept intentionally minimal and
 * decoupled from Spring Data's {@code Page} to keep the JSON contract stable.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
