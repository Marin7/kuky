package com.kuky.backend.quiz.dto;

import java.util.List;
import java.util.UUID;

public record QuizListResponse(List<QuizListItem> quizzes) {
    public record QuizListItem(UUID id, String title, String description, String status, boolean unseen) {}
}
