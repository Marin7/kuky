package com.kuky.backend.learning.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HomeworkItemResponse(
        UUID id,
        String title,
        String instructions,
        LocalDate dueOn,
        String homeworkType,
        String level,
        String status,
        String response,
        Instant submittedAt,
        boolean overdue
) {}
