package com.kuky.backend.admin.dto;

import java.util.UUID;

public record AudioUploadResponse(
        UUID id,
        String originalName,
        String contentType,
        int byteSize
) {}
