package com.kuky.backend.units.dto;

import java.util.UUID;

/**
 * A page activity of the unit, with counts over assigned students:
 * {@code submitted} = {@code awaitingCorrection} + {@code graded}.
 */
public record UnitActivitySummary(
        UUID id,
        int page,
        String title,
        String format,
        String composition,
        int submittedCount,
        int awaitingCorrectionCount,
        int gradedCount,
        boolean hasUnseenSubmissions
) {}
