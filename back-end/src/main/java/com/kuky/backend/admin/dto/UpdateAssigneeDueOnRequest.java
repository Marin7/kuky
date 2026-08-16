package com.kuky.backend.admin.dto;

import java.time.LocalDate;

public record UpdateAssigneeDueOnRequest(
        LocalDate dueOn
) {}
