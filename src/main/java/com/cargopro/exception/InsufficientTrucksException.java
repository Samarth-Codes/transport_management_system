package com.cargopro.exception;

/**
 * Custom exception for when transporter doesn't have enough trucks
 */
public class InsufficientTrucksException extends RuntimeException {
    public InsufficientTrucksException(String message) {
        super(message);
    }
}

