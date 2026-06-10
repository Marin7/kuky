package com.kuky.backend.resources.exception;

public class AlreadyOwnedException extends RuntimeException {
    public AlreadyOwnedException(String message) {
        super(message);
    }
}
