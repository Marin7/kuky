package com.kuky.backend.learning.exception;

public class SubmissionNotAllowedException extends RuntimeException {
    public SubmissionNotAllowedException(String message) {
        super(message);
    }
}
