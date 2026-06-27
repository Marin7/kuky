package com.kuky.backend.admin.dto;

import java.util.List;

/** The materialized availability for one concrete date (the source of truth for that day). */
public record DayAvailabilityDto(
        String date,                 // YYYY-MM-DD
        List<DayWindowDto> windows
) {}
