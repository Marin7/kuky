package com.kuky.backend.admin.dto;

import java.util.UUID;

public record ImageUploadResponse(
        UUID id,
        String contentType,
        int byteSize
) {}
