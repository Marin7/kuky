package com.kuky.backend.quiz.dto;

import java.time.Instant;
import java.util.UUID;

public record QuizAttemptListItem(
        UUID id,
        UUID userId,
        String studentName,
        String email,
        String status,
        Integer scorePercent,
        Instant submittedAt,
        boolean unseen
) {}
