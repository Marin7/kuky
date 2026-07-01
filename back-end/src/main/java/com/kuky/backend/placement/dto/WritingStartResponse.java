package com.kuky.backend.placement.dto;

import java.time.Instant;

public record WritingStartResponse(Instant deadlineAt, Instant serverNow) {}
