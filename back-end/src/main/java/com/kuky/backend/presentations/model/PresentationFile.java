package com.kuky.backend.presentations.model;

import java.time.Instant;
import java.util.UUID;

public record PresentationFile(
        UUID id,
        UUID presentationId,
        String originalName,
        String displayName,
        String contentType,
        int byteSize,
        Instant createdAt,
        byte[] data
) {}
