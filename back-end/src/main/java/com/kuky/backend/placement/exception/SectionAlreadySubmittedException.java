package com.kuky.backend.placement.exception;

public class SectionAlreadySubmittedException extends RuntimeException {
    public SectionAlreadySubmittedException(String message) {
        super(message);
    }
}
