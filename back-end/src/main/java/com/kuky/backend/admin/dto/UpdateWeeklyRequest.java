package com.kuky.backend.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateWeeklyRequest(
        @NotNull @Valid List<WeeklyWindowDto> windows
) {}
