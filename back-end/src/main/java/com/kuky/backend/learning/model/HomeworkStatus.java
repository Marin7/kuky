package com.kuky.backend.learning.model;

/**
 * Lifecycle of a student's homework submission.
 * Manual homework: the student drives PENDING → SUBMITTED; REVIEWED is the
 * teacher's manual-review terminal state. Exercise homework: a single submit
 * transitions straight to GRADED (auto-graded, terminal, locked).
 */
public enum HomeworkStatus {
    PENDING,
    SUBMITTED,
    REVIEWED,
    GRADED
}
