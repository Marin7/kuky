package com.kuky.backend.learning;

/**
 * Authoring/validation caps for structured exercise blanks and word banks
 * ({@code specs/034-multi-correct-blanks}).
 */
public final class ExerciseStructureLimits {

    /** Max accepted typed answers or correct bank ids per blank. */
    public static final int MAX_ACCEPTED_PER_BLANK = 10;

    /** Max items in a DRAG_DROP word bank. */
    public static final int MAX_BANK_ITEMS = 30;

    /** Max numbered {@code (1)}…{@code (N)} items on one SINGLE_CHOICE. */
    public static final int MAX_SINGLE_CHOICE_ITEMS = 20;

    private ExerciseStructureLimits() {}
}
