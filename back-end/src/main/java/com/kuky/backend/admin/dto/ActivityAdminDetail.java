package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A page activity of a unit's PDF (no title of its own: shown as "Actividad – página N"). */
public record ActivityAdminDetail(
        UUID id,
        UUID unitId,
        UUID presentationId,
        int page,
        String format,
        String composition,
        List<HomeworkQuestionDto> questions,
        Instant createdAt,
        Instant updatedAt
) {}
