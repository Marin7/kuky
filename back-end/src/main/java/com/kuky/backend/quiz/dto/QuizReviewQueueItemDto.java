package com.kuky.backend.quiz.dto;

import java.time.Instant;
import java.util.UUID;

/** One quiz attempt awaiting teacher scoring of free-text questions. */
public record QuizReviewQueueItemDto(
        UUID attemptId,
        UUID quizId,
        String quizTitle,
        UUID studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        String studentUsername,
        Instant submittedAt
) {}
