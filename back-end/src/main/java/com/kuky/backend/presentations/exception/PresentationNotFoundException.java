package com.kuky.backend.presentations.exception;

public class PresentationNotFoundException extends RuntimeException {
    public PresentationNotFoundException(String message) {
        super(message);
    }
}
