package com.kuky.backend.learning.service;

import com.kuky.backend.learning.model.HomeworkStatus;

import java.time.LocalDate;

/** Derived overdue: due date present, before today, and work still pending. */
public final class HomeworkDueDates {

    private HomeworkDueDates() {}

    public static boolean overdue(LocalDate dueOn, LocalDate today, String status) {
        return dueOn != null
                && today != null
                && dueOn.isBefore(today)
                && HomeworkStatus.PENDING.name().equals(status);
    }

    /** Null (no deadline) and today are allowed; earlier calendar dates are not. */
    public static void requireNotPast(LocalDate dueOn, LocalDate today) {
        if (dueOn != null && today != null && dueOn.isBefore(today)) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a hoy.");
        }
    }
}
