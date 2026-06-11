package com.kuky.backend.admin.dto;

import java.util.List;

public record UpdateWeeklyResponse(
        List<WeeklyWindowDto> weekly,
        List<BookingConflictDto> bookingConflicts
) {}
