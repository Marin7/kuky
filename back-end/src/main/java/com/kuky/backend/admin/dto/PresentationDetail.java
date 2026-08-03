package com.kuky.backend.admin.dto;

import java.util.List;
import java.util.UUID;

public record PresentationDetail(
        UUID id,
        String title,
        String level,
        List<PresentationFileSummary> files,
        List<StudentResponse> sharedWith
) {}
