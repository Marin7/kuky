package com.kuky.backend.learning.dto;

import com.kuky.backend.admin.dto.PresentationFileSummary;

import java.util.List;
import java.util.UUID;

public record SharedPresentationSummary(
        UUID id,
        String title,
        List<PresentationFileSummary> files,
        UnitRef unit
) {}
