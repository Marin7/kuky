package com.kuky.backend.admin.dto;

import java.util.UUID;

public record ExceptionResponse(
        UUID id,
        String date,
        String kind,
        String startTime,
        String endTime
) {}
