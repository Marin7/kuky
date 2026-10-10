package com.kuky.backend.units.dto;

import java.util.UUID;

/** A quiz shown at the end of a unit, in the teacher's order. */
public record UnitQuizItem(
        UUID id,
        String title,
        int questionCount
) {}
