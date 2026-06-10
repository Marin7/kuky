package com.kuky.backend.resources.exception;

public class NotPurchasableException extends RuntimeException {
    public NotPurchasableException(String message) {
        super(message);
    }
}
