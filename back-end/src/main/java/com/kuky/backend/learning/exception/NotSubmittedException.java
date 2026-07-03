package com.kuky.backend.learning.exception;

/** Thrown when a teacher attempts to review a submission that hasn't been submitted yet (still PENDING). */
public class NotSubmittedException extends RuntimeException {
    public NotSubmittedException(String message) {
        super(message);
    }
}
