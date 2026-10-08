package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A page activity of a unit's PDF. {@code title} null → "Actividad – página N" on the client. */
public record ActivityAdminDetail(
        UUID id,
        UUID unitId,
        UUID presentationId,
        int page,
        String title,
        String format,
        String composition,
        List<HomeworkQuestionDto> questions,
        Instant createdAt,
        Instant updatedAt
) {}
