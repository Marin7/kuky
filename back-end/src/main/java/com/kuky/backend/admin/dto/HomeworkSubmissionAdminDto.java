package com.kuky.backend.admin.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full detail of a Writing homework submission, for the teacher's review screen. */
public record HomeworkSubmissionAdminDto(
        UUID submissionId,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        String assignmentTitle,
        String status,
        List<FormattedTextSegment> response,
        List<FormattedTextSegment> feedback,
        Instant submittedAt,
        Instant reviewedAt
) {}
