package com.kuky.backend.learning.model;

/**
 * Lifecycle of a student's homework submission.
 * Student drives PENDING → SUBMITTED (or straight to GRADED for all-auto).
 * After SUBMITTED / REVIEWED / GRADED the student answer is locked.
 * Manual/WRITE/mixed: teacher finalize → GRADED (REVIEWED is legacy terminal).
 */
public enum HomeworkStatus {
    PENDING,
    SUBMITTED,
    REVIEWED,
    GRADED
}
