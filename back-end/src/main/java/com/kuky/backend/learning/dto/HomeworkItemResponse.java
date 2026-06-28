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
        String format,            // MANUAL | EXERCISE
        String status,
        String response,
        Integer scorePercent,     // present when status == GRADED
        Instant submittedAt,
        boolean overdue,
        String audioUrl,          // listening homework external source (nullable)
        UUID audioFileId,         // listening homework uploaded file (nullable)
        UnitRef unit              // owning unit for grouping (nullable for legacy/unattached)
) {}
