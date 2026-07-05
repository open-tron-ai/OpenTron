package org.opentron.backend.exception;

/**
 * Exception thrown when an operation cannot proceed due to invalid state.
 */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
