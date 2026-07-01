package com.kuky.backend.placement.dto;

import java.time.Instant;

public record StartSectionResponse(String skill, Instant deadlineAt, Instant serverNow) {}
