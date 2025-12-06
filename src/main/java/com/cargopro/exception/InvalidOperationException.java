package com.cargopro.exception;

/**
 * Custom exception for invalid business operations
 * (e.g., bidding on booked load, accepting bid when load is cancelled)
 */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}

