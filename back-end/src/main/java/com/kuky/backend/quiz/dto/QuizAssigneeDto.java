package com.kuky.backend.quiz.dto;

import java.util.List;
import java.util.UUID;

public record QuizAssigneeDto(
        UUID id,
        String name,
        String email
) {}
