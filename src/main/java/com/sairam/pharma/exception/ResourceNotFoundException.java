package com.sairam.pharma.exception;

// ================================================================
// Custom Exception Classes
//
// WHAT ARE CUSTOM EXCEPTIONS?
// Java has built-in exceptions like NullPointerException.
// We create OUR OWN to give meaningful error messages.
//
// WHY NOT JUST USE RuntimeException EVERYWHERE?
// Because then you can't tell WHY something failed.
// ResourceNotFoundException clearly says "you asked for something
// that doesn't exist" — much clearer than a generic error.
//
// These exceptions are caught in GlobalExceptionHandler
// and turned into clean JSON error responses.
// ================================================================

public class ResourceNotFoundException extends RuntimeException {
    // Thrown when: GET /agencies/999 but ID 999 doesn't exist
    public ResourceNotFoundException(String message) {
        super(message);
    }
}