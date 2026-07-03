package com.kuky.backend.scheduling.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingSummary(
        UUID id,
        Instant slotStart,
        Instant slotEnd,
        String status,
        String zoomJoinUrl,
        boolean cancellable,
        boolean isCompanionStudent
) {}
