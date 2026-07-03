package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** One Writing homework submission awaiting teacher feedback (the cross-student review queue). */
public record HomeworkReviewQueueItemDto(
        UUID submissionId,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        String assignmentTitle,
        Instant submittedAt
) {}
