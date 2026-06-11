package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PresentationSummary(
        UUID id,
        String title,
        String level,
        boolean hasFile,
        String originalFileName,
        List<String> sharedWithIds,
        Instant updatedAt
) {}
