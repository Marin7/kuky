package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;
import java.util.UUID;

/** Student/teacher view of one FREE_TEXT answer (includes prompt snapshot). */
public record ManualAnswerViewDto(
        UUID questionId,
        String promptSnapshot,
        String text,
        List<FormattedTextSegment> formatted,
        Integer teacherScorePercent,
        Double score
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
                score);
    }
}
