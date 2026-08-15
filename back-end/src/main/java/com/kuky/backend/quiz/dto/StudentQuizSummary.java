package com.kuky.backend.quiz.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentQuizSummary(
        UUID quizId,
        UUID attemptId,
        String title,
        String status,
        Integer scorePercent,
        Instant submittedAt,
        List<QuizSkillScoreDto> skills,
        boolean unseen
) {}
