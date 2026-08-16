package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentProfileHomeworkDto(
        UUID id,
        String title,
        String status,
        Instant submittedAt,
        boolean needsReview,
        UUID submissionId,
        Integer scorePercent,  // set for GRADED exercises; null otherwise
        boolean hasTeacherFeedback,
        boolean unseen,
        String format,
        LocalDate dueOn,
        boolean overdue
) {}
