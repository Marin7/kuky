package com.kuky.backend.learning.dto;

import com.kuky.backend.admin.dto.PresentationFileSummary;

import java.util.List;
import java.util.UUID;

public record SharedPresentationSummary(
        UUID id,
        String title,
        List<PresentationFileSummary> files,
        UnitRef unit,
        List<ActivitySummary> activities,
        /** For a unit PDF: one title per page (index 0 = page 1); empty for shared presentations. */
        List<String> pageTitles
) {}
