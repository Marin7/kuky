package com.kuky.backend.admin.dto;

import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.model.FormattedTextSegment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full detail of a MANUAL / MIXED / WRITE homework submission, for the teacher's review screen. */
public record HomeworkSubmissionAdminDto(
        UUID submissionId,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        String assignmentTitle,
        String status,
        String format,
        String composition,
        String reviewModel,
        List<FormattedTextSegment> response,
        List<ManualAnswerViewDto> answers,
        List<FormattedTextSegment> feedback,
        String feedbackText,
        Integer scorePercent,
        Instant submittedAt,
        Instant reviewedAt
) {}
