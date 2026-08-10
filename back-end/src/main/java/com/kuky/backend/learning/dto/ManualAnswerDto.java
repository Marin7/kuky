package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/** One FREE_TEXT answer in a MANUAL multi-question submit body. */
public record ManualAnswerDto(
        UUID questionId,
        String text
) {}
