package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/**
 * One manual answer in a MANUAL multi-question submit body: {@code text} for FREE_TEXT,
 * {@code continuations} (one per starter) for SENTENCE_STARTER.
 */
public record ManualAnswerDto(
        UUID questionId,
        String text,
        List<String> continuations
) {
    public ManualAnswerDto(UUID questionId, String text) {
        this(questionId, text, null);
    }
}
