package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** One absolute availability window on a date. Times are "HH:mm" (teacher local). */
public record DayWindowDto(
        @NotBlank String startTime,
        @NotBlank String endTime
) {}
