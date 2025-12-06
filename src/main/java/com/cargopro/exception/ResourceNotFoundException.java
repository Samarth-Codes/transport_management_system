package com.cargopro.exception;

/**
 * Custom exception for when a resource (Load, Transporter, etc.) is not found
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

