package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Teacher-initiated booking for a student at an arbitrary wall-clock time in the teacher's
 * timezone — availability windows and lead time are not required; overlap still is.
 */
public record CreateAdminBookingRequest(
        @NotNull UUID studentId,
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        @NotNull Integer durationMinutes
) {}
