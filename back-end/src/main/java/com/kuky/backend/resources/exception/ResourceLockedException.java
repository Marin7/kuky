package com.kuky.backend.resources.exception;

public class ResourceLockedException extends RuntimeException {
    public ResourceLockedException(String message) {
        super(message);
    }
}
