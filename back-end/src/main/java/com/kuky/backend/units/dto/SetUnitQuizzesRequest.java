package com.kuky.backend.units.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** The unit's quizzes, in the order they appear at the end of the unit. */
public record SetUnitQuizzesRequest(
        @NotNull List<UUID> quizIds
) {}
