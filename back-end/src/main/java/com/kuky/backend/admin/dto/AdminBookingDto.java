package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminBookingDto(
        UUID id,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        Instant slotStart,
        Instant slotEnd,
        String zoomJoinUrl
) {}
