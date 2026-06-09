package com.kuky.backend.scheduling.exception;

public class MeetingProvisioningException extends RuntimeException {
    public MeetingProvisioningException(String message) {
        super(message);
    }

    public MeetingProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
