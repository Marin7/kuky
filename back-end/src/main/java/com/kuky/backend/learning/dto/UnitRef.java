package com.kuky.backend.learning.dto;

import java.util.UUID;

/** Lightweight unit descriptor surfaced to students for grouping their content. */
public record UnitRef(UUID id, String level, String subject, int position) {}
