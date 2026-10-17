package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.service.SentenceStarters;

import java.util.List;
import java.util.UUID;

/**
 * Student/teacher view of one manual answer (includes prompt snapshot). For SENTENCE_STARTER,
 * {@code starters} is the submit-time snapshot; null for FREE_TEXT.
 */
public record ManualAnswerViewDto(
        UUID questionId,
        String promptSnapshot,
        String text,
        List<FormattedTextSegment> formatted,
        Integer teacherScorePercent,
        Double score,
        List<String> starters
) {
    public static ManualAnswerViewDto fromStored(UUID questionId, String promptSnapshot, String answerText) {
        return fromStored(questionId, promptSnapshot, answerText, null, null);
    }

    public static ManualAnswerViewDto fromStored(UUID questionId, String promptSnapshot, String answerText,
                                                 Integer teacherScorePercent, Double score) {
        return new ManualAnswerViewDto(
                questionId,
                promptSnapshot,
                FormattedTextSegment.storedPlainWording(answerText),
                FormattedTextSegment.tryParseFormatted(answerText),
                teacherScorePercent,
                score,
                null);
    }

    /** Adds the SENTENCE_STARTER snapshot from the stored {@code answer_json}, if any. */
    public ManualAnswerViewDto withSentenceStarters(String answerJson) {
        List<String> snapshot = SentenceStarters.snapshotStarters(answerJson);
        if (snapshot == null) return this;
        return new ManualAnswerViewDto(
                questionId, promptSnapshot, text, formatted, teacherScorePercent, score, snapshot);
    }
}
