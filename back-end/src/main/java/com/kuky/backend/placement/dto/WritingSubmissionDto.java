package com.kuky.backend.placement.dto;

import java.time.Instant;
import java.util.UUID;

public record WritingSubmissionDto(UUID id, String body, Instant submittedAt) {}
