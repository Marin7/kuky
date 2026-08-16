package com.kuky.backend.learning.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/**
 * One styled run of plain text within a Writing homework answer or teacher
 * feedback. Deliberately has no field capable of holding scripts, links, or
 * images — the format is safe by construction, not by runtime sanitization.
 * Used for both {@code homework_submissions.response_text} and {@code feedback}
 * (each stored as a JSON-encoded array of segments).
 */
public record FormattedTextSegment(String text, String color, String highlight, Boolean strike) {

    public static final int MAX_VISIBLE_LENGTH = 2000;
    public static final int MAX_MANUAL_FEEDBACK_LENGTH = 500;

    private static final Set<String> COLORS = Set.of("red", "green", "blue", "neutral");
    private static final Set<String> HIGHLIGHTS = Set.of("yellow", "green", "pink");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Rejects empty content, empty/blank segment text, unknown colors, and answers over the visible-length limit. */
    public static void validate(List<FormattedTextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío.");
        }
        int visibleLength = 0;
        for (FormattedTextSegment segment : segments) {
            if (segment.text() == null || segment.text().isEmpty()) {
                throw new IllegalArgumentException("El contenido no puede estar vacío.");
            }
            if (segment.color() != null && !COLORS.contains(segment.color())) {
                throw new IllegalArgumentException("Color de texto no válido.");
            }
            if (segment.highlight() != null && !HIGHLIGHTS.contains(segment.highlight())) {
                throw new IllegalArgumentException("Color de resaltado no válido.");
            }
            visibleLength += segment.text().length();
        }
        if (visibleLength > MAX_VISIBLE_LENGTH) {
            throw new IllegalArgumentException("El contenido es demasiado largo (máximo 2000 caracteres).");
        }
    }

    public static String toJson(List<FormattedTextSegment> segments) {
        if (segments == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(segments);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el contenido.", e);
        }
    }

    public static List<FormattedTextSegment> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, FormattedTextSegment.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el contenido.", e);
        }
    }

    public static List<FormattedTextSegment> tryParseFormatted(String raw) {
        if (raw == null || !raw.strip().startsWith("[")) {
            return null;
        }
        try {
            return fromJson(raw);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static String plainText(List<FormattedTextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (FormattedTextSegment segment : segments) {
            if (segment != null && segment.text() != null) {
                joined.append(segment.text());
            }
        }
        return joined.toString();
    }

    public static String storedPlainWording(String raw) {
        List<FormattedTextSegment> formatted = tryParseFormatted(raw);
        return formatted == null ? raw : plainText(formatted);
    }

    /**
     * Encode plain exercise teacher feedback for {@code homework_submissions.feedback}.
     * Stores a single unformatted FormattedText segment (or {@code null} when cleared).
     * Whitespace-only input clears. Visible length must be ≤ {@link #MAX_VISIBLE_LENGTH}.
     */
    public static String encodePlainFeedback(String plain) {
        return encodePlainFeedback(plain, MAX_VISIBLE_LENGTH);
    }

    public static String encodePlainFeedback(String plain, int maxLen) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        String text = plain.strip();
        if (text.length() > maxLen) {
            throw new IllegalArgumentException(
                    "El contenido es demasiado largo (máximo " + maxLen + " caracteres).");
        }
        return toJson(List.of(new FormattedTextSegment(text, null, null, null)));
    }

    /** Decode stored feedback JSON to a plain string; {@code null} when absent/empty. */
    public static String decodePlainFeedback(String json) {
        List<FormattedTextSegment> segments = fromJson(json);
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        String joined = plainText(segments);
        return joined.isEmpty() ? null : joined;
    }

    /** Whether stored feedback JSON represents a non-empty teacher comment. */
    public static boolean hasTeacherFeedback(String json) {
        return decodePlainFeedback(json) != null;
    }

    /** Whether stored formatted text has teacher color, highlight, or strike marks. */
    public static boolean hasStyleMarks(String stored) {
        List<FormattedTextSegment> segs = tryParseFormatted(stored);
        if (segs == null) {
            return false;
        }
        for (FormattedTextSegment s : segs) {
            if (s == null) {
                continue;
            }
            if (s.color() != null || s.highlight() != null || Boolean.TRUE.equals(s.strike())) {
                return true;
            }
        }
        return false;
    }
}
