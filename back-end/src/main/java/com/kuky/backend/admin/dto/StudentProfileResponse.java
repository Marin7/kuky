package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String username,
        UUID avatarImageId,
        Instant createdAt,
        List<StudentProfileBookingDto> bookings,
        List<StudentProfileHomeworkDto> homeworks,
        List<StudentProfilePresentationDto> presentations,
        StudentProgressDto progress
) {}
