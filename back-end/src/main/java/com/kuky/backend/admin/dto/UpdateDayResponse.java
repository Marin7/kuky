package com.kuky.backend.admin.dto;

import java.util.List;

public record UpdateDayResponse(
        String date,
        List<DayWindowDto> windows,
        List<BookingConflictDto> bookingConflicts
) {}
