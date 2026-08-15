package com.kuky.backend.quiz.dto;

import java.util.List;
import java.util.UUID;

public record QuizAdminDetail(
        UUID id,
        String title,
        String description,
        List<QuizQuestionDto> questions,
        List<QuizAssigneeDto> assignees
) {}
