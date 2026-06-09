package com.kuky.backend.scheduling.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        Instant slotStart,
        Instant slotEnd,
        int durationMinutes,
        String status,
        String zoomJoinUrl,
        Instant createdAt
) {}
