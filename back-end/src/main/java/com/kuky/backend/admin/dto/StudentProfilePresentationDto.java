package com.kuky.backend.admin.dto;

import java.util.UUID;

public record StudentProfilePresentationDto(
        UUID id,
        String title,
        String level
) {}
