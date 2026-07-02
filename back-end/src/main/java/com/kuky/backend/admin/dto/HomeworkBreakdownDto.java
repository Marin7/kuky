package com.kuky.backend.admin.dto;

public record HomeworkBreakdownDto(
        int pending,
        int submitted,
        int completed
) {}
