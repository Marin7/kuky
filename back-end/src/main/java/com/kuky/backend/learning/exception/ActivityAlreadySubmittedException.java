package com.kuky.backend.learning.exception;

public class ActivityAlreadySubmittedException extends RuntimeException {
    public ActivityAlreadySubmittedException(String message) {
        super(message);
    }
}
