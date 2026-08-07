package com.kuky.backend.admin.dto;

/** Activity fulfillment breakdown on the student profile progress overview. */
public record ActivityBreakdownDto(
        int pending,
        int submitted,
        int completed
) {}
