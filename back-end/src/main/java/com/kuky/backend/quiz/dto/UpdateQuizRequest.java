package com.kuky.backend.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateQuizRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull List<QuizQuestionDto> questions
) {}
