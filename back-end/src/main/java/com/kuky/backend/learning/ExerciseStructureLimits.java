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

    /** Max wrong words on one SPOT_WRONG_WORDS question ({@code specs/054-spot-wrong-words}). */
    public static final int MAX_SPOT_ERRORS = 30;

    /** Max passage length (characters) of a SPOT_WRONG_WORDS question. */
    public static final int MAX_SPOT_TEXT_CHARS = 3000;

    /** Max length of one SPOT_WRONG_WORDS correction. */
    public static final int MAX_SPOT_CORRECTION_CHARS = 100;

    /** Max starters on one SENTENCE_STARTER question ({@code specs/057-sentence-starter-answer}). */
    public static final int MAX_STARTERS = 10;

    /** Max length of one SENTENCE_STARTER starter (after trailing-ellipsis removal). */
    public static final int MAX_STARTER_CHARS = 80;

    /** Max length of one SENTENCE_STARTER model answer. */
    public static final int MAX_MODEL_ANSWER_CHARS = 300;

    /** Max length of one student continuation. */
    public static final int MAX_CONTINUATION_CHARS = 500;

    /** Max length of a whole composed answer (the teacher-annotation limit). */
    public static final int MAX_SENTENCE_STARTER_ANSWER_CHARS = 2000;

    private ExerciseStructureLimits() {}
}
