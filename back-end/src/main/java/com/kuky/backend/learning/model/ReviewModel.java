package com.kuky.backend.learning.model;

/**
 * Discriminator for MANUAL (homework/activity) teacher reviews.
 * {@code null} until first review; {@link #LEGACY_RICH} is frozen; {@link #ANNOTATED} is re-editable.
 */
public enum ReviewModel {
    LEGACY_RICH,
    ANNOTATED
}
