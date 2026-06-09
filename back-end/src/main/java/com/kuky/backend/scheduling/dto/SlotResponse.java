package com.kuky.backend.scheduling.dto;

import java.time.Instant;

public record SlotResponse(
        Instant start,
        Instant end,
        String status
) {}
