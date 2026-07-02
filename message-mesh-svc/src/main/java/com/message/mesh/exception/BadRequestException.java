package com.message.mesh.exception;

/**
 * Thrown for invalid client input or business-rule violations.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
