package com.message.mesh.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error envelope returned to clients.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String requestId,
        Map<String, String> fieldErrors
) {
}
