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
}
