package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** A weekly availability window. {@code id} is null on input, populated on output. */
public record WeeklyWindowDto(
        UUID id,
        @Min(1) @Max(7) int dayOfWeek,
        @NotBlank String startTime,
        @NotBlank String endTime
) {}
