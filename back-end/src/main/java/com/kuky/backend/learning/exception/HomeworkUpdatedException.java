package com.kuky.backend.learning.exception;

/** Student submit against a homework whose live content changed since they opened it. */
public class HomeworkUpdatedException extends RuntimeException {

    public HomeworkUpdatedException(String message) {
        super(message);
    }
}
