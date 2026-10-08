package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** One activity submission awaiting a teacher grade. */
public record ActivityReviewQueueItemDto(
        UUID submissionId,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        String assignmentTitle,
        Instant submittedAt,
        boolean unseen,
        UUID activityId,
        UUID unitId,
        int page
) {}
