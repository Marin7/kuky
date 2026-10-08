package com.kuky.backend.units.dto;

import java.time.Instant;
import java.util.UUID;

/** One (activity × assigned student) cell; {@code submissionId} null while PENDING. */
public record UnitActivityProgressRow(
        UUID activityId,
        int page,
        UUID studentId,
        String status,
        Integer scorePercent,
        UUID submissionId,
        Instant submittedAt,
        boolean unseen
) {}
