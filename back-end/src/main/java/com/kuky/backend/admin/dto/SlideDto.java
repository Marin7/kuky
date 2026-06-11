package com.kuky.backend.admin.dto;

import java.util.UUID;

public record SlideDto(
        UUID id,
        String heading,
        String body,
        UUID imageId,
        int sortOrder
) {}
