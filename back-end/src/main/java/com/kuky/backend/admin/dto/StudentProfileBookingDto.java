package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record StudentProfileBookingDto(
        UUID id,
        Instant slotStart,
        Instant slotEnd,
        String status,
        String zoomJoinUrl
) {}
