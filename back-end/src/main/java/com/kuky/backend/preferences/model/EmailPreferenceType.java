package com.kuky.backend.preferences.model;

/**
 * The occasions on which the site may email a student.
 *
 * <p>Constant names are the wire value used by {@code /api/v1/me/email-preferences},
 * so renaming one is a breaking API change.
 *
 * <p>Declaration order is the order options are presented to the student.
 */
public enum EmailPreferenceType {

    /** A homework became available to this student for the first time. */
    NEW_HOMEWORK_ASSIGNED
}
