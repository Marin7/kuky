package com.kuky.backend.learning.dto;

import java.util.UUID;

/** Compact activity summary nested under a shared presentation in the learning overview. */
public record ActivitySummary(
        UUID id,
        String title,
        String format,
        int position,
        String status,
        Integer scorePercent,
        UUID triggerFileId,
        Integer triggerPage,
        String instructionsText,
        String youtubeUrl,
        UUID imageId
) {}
