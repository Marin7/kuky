package com.kuky.backend.learning.exception;

/** Thrown when a teacher attempts to save feedback on a submission that is already REVIEWED (terminal). */
public class AlreadyReviewedException extends RuntimeException {
    public AlreadyReviewedException(String message) {
        super(message);
    }
}
