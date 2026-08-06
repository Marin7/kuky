package com.kuky.backend.learning.model;

/**
 * The kind of an exercise question.
 * <ul>
 *   <li>{@code SINGLE_CHOICE} — radio; scored 0/1 (selected set must equal the one correct option).</li>
 *   <li>{@code MULTI_CHOICE} — checkboxes; partial credit over all options.</li>
 *   <li>{@code MULTI_BLANK} — passage with 1–20 {@code ___} blanks; typed answers.</li>
 *   <li>{@code DRAG_DROP} — passage with ≥2 blanks; word bank placed by id (bank order = correct order).</li>
 *   <li>{@code TABLE_FILL} — grid of fixed/blank cells; typed blank cells.</li>
 *   <li>{@code MATCHING} — left↔right pairs with optional distractors.</li>
 *   <li>{@code TRUE_FALSE} — fixed true/false options; scored 0/1 like single choice.</li>
 * </ul>
 */
public enum QuestionKind {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    MULTI_BLANK,
    DRAG_DROP,
    TABLE_FILL,
    MATCHING,
    TRUE_FALSE;

    /** Kinds that store answer keys in {@code structure_json} rather than options rows. */
    public boolean isStructured() {
        return this == MULTI_BLANK || this == DRAG_DROP || this == TABLE_FILL || this == MATCHING;
    }
}
