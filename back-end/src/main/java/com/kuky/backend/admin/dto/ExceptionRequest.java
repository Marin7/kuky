package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExceptionRequest(
        @NotBlank String date,                       // YYYY-MM-DD
        @Pattern(regexp = "BLOCK|OPEN") String kind,
        @NotBlank String startTime,
        @NotBlank String endTime
) {}
