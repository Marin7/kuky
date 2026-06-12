package com.kuky.backend.learning.model;

/**
 * The kind of an exercise question.
 * <ul>
 *   <li>{@code SINGLE_CHOICE} — radio; scored 0/1 (selected set must equal the one correct option).</li>
 *   <li>{@code MULTI_CHOICE} — checkboxes; partial credit over all options.</li>
 *   <li>{@code FILL_BLANK} — one text blank; matched against accepted answers (accent-exact).</li>
 * </ul>
 */
public enum QuestionKind {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    FILL_BLANK
}
