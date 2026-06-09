package com.kuky.backend.scheduling.dto;

import java.util.List;

public record MyBookingsResponse(
        List<BookingSummary> upcoming,
        List<BookingSummary> past
) {}
