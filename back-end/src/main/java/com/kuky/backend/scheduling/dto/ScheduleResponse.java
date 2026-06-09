package com.kuky.backend.scheduling.dto;

import java.time.Instant;
import java.util.List;

public record ScheduleResponse(
        String teacherTimezone,
        Instant horizonStart,
        Instant horizonEnd,
        List<SlotResponse> slots
) {}
