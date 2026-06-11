package com.kuky.backend.learning.dto;

import java.util.UUID;

public record SharedPresentationSummary(
        UUID id,
        String title,
        boolean hasFile
) {}
