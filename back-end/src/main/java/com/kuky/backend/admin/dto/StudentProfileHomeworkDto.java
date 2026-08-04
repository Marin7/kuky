package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record StudentProfileHomeworkDto(
        UUID id,
        String title,
        String status,
        Instant submittedAt,
        boolean needsReview,
        UUID submissionId,
        Integer scorePercent  // set for GRADED exercises; null otherwise
) {}
