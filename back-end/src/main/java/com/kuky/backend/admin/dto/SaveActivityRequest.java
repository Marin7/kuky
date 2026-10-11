package com.kuky.backend.admin.dto;

import java.util.List;

/** Edits a page activity; the page itself never changes (unmark and re-mark instead). */
public record SaveActivityRequest(
        List<HomeworkQuestionDto> questions
) {}
