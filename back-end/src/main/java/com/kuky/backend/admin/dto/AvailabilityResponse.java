package com.kuky.backend.admin.dto;

import java.util.List;

/**
 * Admin availability snapshot: the general weekly template ({@code weekly}) plus the materialized
 * per-date source of truth for the current horizon ({@code days}).
 */
public record AvailabilityResponse(
        List<WeeklyWindowDto> weekly,
        List<DayAvailabilityDto> days
) {}
