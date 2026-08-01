package com.kuky.backend.auth.exception;

/** Thrown when a non-student account attempts to update interests. */
public class InterestsAccessDeniedException extends RuntimeException {
    public InterestsAccessDeniedException(String message) {
        super(message);
    }
}
