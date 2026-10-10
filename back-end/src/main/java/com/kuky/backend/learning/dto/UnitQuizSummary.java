package com.kuky.backend.learning.dto;

import java.util.UUID;

/**
 * A quiz shown at the end of one of the student's units, in the teacher's order.
 * {@code status}: AVAILABLE (not started), IN_PROGRESS, SUBMITTED or GRADED.
 */
public record UnitQuizSummary(
        UUID id,
        String title,
        UnitRef unit,
        String status,
        boolean unseen
) {}
