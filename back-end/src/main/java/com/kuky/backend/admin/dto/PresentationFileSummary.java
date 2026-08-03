package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record PresentationFileSummary(
        UUID id,
        String displayName,
        String originalName,
        String contentType,
        int byteSize,
        Instant createdAt
) {}
