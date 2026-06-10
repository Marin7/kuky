package com.kuky.backend.learning.model;

/**
 * Lifecycle of a student's homework submission.
 * The student drives PENDING → SUBMITTED. REVIEWED is reserved for the future
 * teacher backoffice and is read-only to students.
 */
public enum HomeworkStatus {
    PENDING,
    SUBMITTED,
    REVIEWED
}
