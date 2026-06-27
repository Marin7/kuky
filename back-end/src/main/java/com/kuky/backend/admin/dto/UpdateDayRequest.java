package com.kuky.backend.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Replace all windows for a single date. An empty list clears the date (fully off that week). */
public record UpdateDayRequest(
        @NotNull @Valid List<DayWindowDto> windows
) {}
