package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
        List<FormattedTextSegment> response,
        List<FormattedTextSegment> feedback,   // teacher's formatted feedback, present once REVIEWED
        Integer scorePercent,     // present when status == GRADED
        Instant submittedAt,
        boolean overdue,
        String audioUrl,          // listening homework external source (nullable)
        UUID audioFileId,         // listening homework uploaded file (nullable)
        UnitRef unit              // owning unit for grouping (nullable for legacy/unattached)
) {}
