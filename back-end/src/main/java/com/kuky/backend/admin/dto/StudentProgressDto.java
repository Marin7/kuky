package com.kuky.backend.admin.dto;

import java.util.List;

public record StudentProgressDto(
        List<UnitProgressDto> units,
        HomeworkBreakdownDto homeworkBreakdown,
        int attendedClasses
) {}
