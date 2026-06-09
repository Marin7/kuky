package com.kuky.backend.scheduling.exception;

public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
