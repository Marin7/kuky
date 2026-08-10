package com.kuky.backend.learning.model;

/**
 * API-facing discriminator derived from homework type + question kinds.
 * Prefer this over {@link HomeworkFormat} for UI/service branching.
 */
public enum HomeworkComposition {
    WRITE,
    ALL_MANUAL,
    ALL_AUTO,
    MIXED
}
