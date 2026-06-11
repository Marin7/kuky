package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingConflictDto(
        UUID bookingId,
        String studentEmail,
        Instant slotStart
) {}
