package com.kuky.backend.admin.dto;

import java.util.List;

public record AvailabilityResponse(
        List<WeeklyWindowDto> weekly,
        List<ExceptionResponse> exceptions
) {}
