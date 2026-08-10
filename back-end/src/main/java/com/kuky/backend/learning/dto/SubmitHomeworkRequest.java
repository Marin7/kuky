package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;

/**
 * MANUAL homework/activity submit body.
 * WRITE uses {@code response}; multi FREE_TEXT MANUAL uses {@code answers}.
 */
public record SubmitHomeworkRequest(
        List<FormattedTextSegment> response,
        List<ManualAnswerDto> answers
) {}
