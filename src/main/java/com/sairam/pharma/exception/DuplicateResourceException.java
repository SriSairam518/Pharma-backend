package com.sairam.pharma.exception;

public class DuplicateResourceException extends RuntimeException {
    // Thrown when: trying to create an agency with a name that already exists
    public DuplicateResourceException(String message) {
        super(message);
    }
}