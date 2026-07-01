package com.kuky.backend.placement.dto;

import java.util.List;
import java.util.UUID;

public record PlacementTestResponse(
        UUID attemptId, // null if the user has never started one
        List<SectionDto> sections
) {}
