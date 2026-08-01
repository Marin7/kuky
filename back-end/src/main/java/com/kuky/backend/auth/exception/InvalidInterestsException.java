package com.kuky.backend.auth.exception;

public class InvalidInterestsException extends RuntimeException {

    private final String errorCode;

    public InvalidInterestsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
