package com.kuky.backend.admin.dto;

import java.util.UUID;

/** One page activity of a unit assigned to the student, with that student's submission state. */
public record StudentProfileUnitActivityDto(
        UUID unitId,
        String unitLevel,
        String unitSubject,
        UUID activityId,
        int page,
        String title,
        String format,
        String status,
        Integer scorePercent,
        UUID submissionId,
        boolean unseen
) {}
