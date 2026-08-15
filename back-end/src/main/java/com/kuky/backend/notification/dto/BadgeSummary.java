package com.kuky.backend.notification.dto;

public record BadgeSummary(
        boolean panel,
        boolean homework,
        boolean quiz,
        boolean learning
) {
    public static BadgeSummary none() {
        return new BadgeSummary(false, false, false, false);
    }
}
