package com.kuky.backend.learning.exception;

import org.springframework.http.HttpStatus;

public class SubmissionNotAllowedException extends RuntimeException {

    private final HttpStatus status;

    public SubmissionNotAllowedException(String message) {
        this(message, HttpStatus.CONFLICT);
    }

    public SubmissionNotAllowedException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
