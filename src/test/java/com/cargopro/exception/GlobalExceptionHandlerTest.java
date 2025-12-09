package com.cargopro.exception;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequestWithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("object", "field1", "Field1 is required");
        FieldError fieldError2 = new FieldError("object", "field2", "Field2 must be positive");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Field1 is required", response.getBody().get("field1"));
        assertEquals("Field2 must be positive", response.getBody().get("field2"));
    }

    @Test
    void handleResourceNotFoundException_ShouldReturnNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Load not found with id: 123");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Load not found with id: 123", response.getBody().get("error"));
    }

    @Test
    void handleInvalidStatusTransitionException_ShouldReturnBadRequest() {
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException("Cannot cancel a booked load");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleInvalidStatusTransitionException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Cannot cancel a booked load", response.getBody().get("error"));
    }

    @Test
    void handleInsufficientCapacityException_ShouldReturnBadRequest() {
        InsufficientCapacityException ex = new InsufficientCapacityException("Not enough trucks available");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleInsufficientCapacityException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not enough trucks available", response.getBody().get("error"));
    }

    @Test
    void handleConflictException_ShouldReturnConflict() {
        ConflictException ex = new ConflictException("Resource already exists");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleConflictException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource already exists", response.getBody().get("error"));
    }

    @Test
    void handleLoadAlreadyBookedException_ShouldReturnConflict() {
        LoadAlreadyBookedException ex = new LoadAlreadyBookedException("Load is already booked");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleLoadAlreadyBookedException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Load is already booked", response.getBody().get("error"));
    }

    @Test
    void handleOptimisticLockException_ShouldReturnConflict() {
        StaleObjectStateException ex = mock(StaleObjectStateException.class);

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleOptimisticLockException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").contains("modified by another request"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").contains("unexpected error"));
        assertTrue(response.getBody().get("error").contains("Something went wrong"));
    }

    @Test
    void handleGenericException_WithNullPointerException_ShouldReturnInternalServerError() {
        NullPointerException ex = new NullPointerException("Null reference");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").contains("Null reference"));
    }
}
