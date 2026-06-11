package com.kuky.backend.presentations.model;

import java.util.UUID;

public record PresentationFile(
        UUID presentationId,
        String originalName,
        String contentType,
        int byteSize,
        byte[] data
) {}
