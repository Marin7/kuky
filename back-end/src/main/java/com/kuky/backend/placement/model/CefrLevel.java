package com.kuky.backend.placement.model;

/**
 * CEFR levels used to tag authored questions. The computed per-skill/overall
 * result may also be the string {@code "A0"} ("below A1") — that is not an
 * enum value here since it never tags a question, only a computed outcome.
 */
public enum CefrLevel {
    A1, A2, B1, B2, C1, C2
}
