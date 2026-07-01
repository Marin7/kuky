package com.kuky.backend.placement.dto;

public record PlacementConfigDto(
        int readingTimeSeconds,
        int listeningTimeSeconds,
        int grammarTimeSeconds,
        int writingTimeSeconds,
        String writingPrompt
) {}
