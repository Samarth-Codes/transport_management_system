package com.cargopro.exception;

public class LoadAlreadyBookedException extends RuntimeException {
    public LoadAlreadyBookedException(String message) {
        super(message);
    }
}
