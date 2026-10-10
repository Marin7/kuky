package com.kuky.backend.quiz.dto;

import java.util.List;
import java.util.UUID;

public record QuizAdminListItem(
        UUID id,
        String title,
        int questionCount,
        int assigneeCount,
        int attemptCount,
        boolean hasUnseenAttempts,
        UUID unitId,       // unit the quiz is shown at the end of (null when standalone)
        String unitLabel   // "A1 · La familia"
) {}
