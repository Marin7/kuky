package com.kuky.backend.placement.dto;

/** Minimum score (%) a section must reach to be awarded this CEFR level. */
public record LevelThresholdDto(
        String level,
        int minScorePercent
) {}
