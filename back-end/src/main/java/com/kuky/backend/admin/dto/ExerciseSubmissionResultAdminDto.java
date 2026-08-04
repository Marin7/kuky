package com.kuky.backend.admin.dto;

import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;

import java.util.List;
import java.util.UUID;

/** Graded exercise detail for the teacher — questions + per-answer result. */
public record ExerciseSubmissionResultAdminDto(
        UUID submissionId,
        UUID assignmentId,
        String assignmentTitle,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        List<ExerciseQuestionDto> questions,
        ExerciseResultResponse result
) {}
