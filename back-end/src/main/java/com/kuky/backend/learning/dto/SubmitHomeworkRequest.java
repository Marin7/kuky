package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;

public record SubmitHomeworkRequest(
        List<FormattedTextSegment> response
) {}
