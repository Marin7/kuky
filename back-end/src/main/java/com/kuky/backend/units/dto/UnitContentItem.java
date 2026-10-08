package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.HomeworkAdminItem;

/**
 * One entry in a unit's mixed content sequence: the unit's own PDF ({@code homework} null)
 * or one of its homeworks.
 */
public record UnitContentItem(
        String type,
        int unitPosition,
        HomeworkAdminItem homework
) {
    public static final String PDF = "PDF";
    public static final String HOMEWORK = "HOMEWORK";
}
