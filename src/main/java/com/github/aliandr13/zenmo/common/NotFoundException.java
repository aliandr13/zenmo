package com.github.aliandr13.zenmo.common;

/**
 * Thrown when a requested resource is not found.
 */
public class NotFoundException extends RuntimeException {
    /**
     * Constructor.
     */
    public NotFoundException(String message) {
        super(message);
    }
}

