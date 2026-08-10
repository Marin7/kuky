package com.kuky.backend.learning.dto;

import java.util.UUID;

/** Student/teacher view of one FREE_TEXT answer (includes prompt snapshot). */
public record ManualAnswerViewDto(
        UUID questionId,
        String promptSnapshot,
        String text
) {}
