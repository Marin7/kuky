package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.PresentationSummary;

/**
 * One entry in a unit's mixed content sequence.
 * Exactly one of {@code presentation} / {@code homework} is non-null, matching {@code type}.
 */
public record UnitContentItem(
        String type,
        int unitPosition,
        PresentationSummary presentation,
        HomeworkAdminItem homework
) {
    public static final String PRESENTATION = "PRESENTATION";
    public static final String HOMEWORK = "HOMEWORK";
}
