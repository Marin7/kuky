package com.kuky.backend.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.kuky.backend.learning.ExerciseStructureLimits.MAX_CONTINUATION_CHARS;
import static com.kuky.backend.learning.ExerciseStructureLimits.MAX_SENTENCE_STARTER_ANSWER_CHARS;
import static com.kuky.backend.learning.ExerciseStructureLimits.MAX_STARTERS;
import static com.kuky.backend.learning.ExerciseStructureLimits.MAX_STARTER_CHARS;

/**
 * SENTENCE_STARTER helpers shared by authoring validation and the homework/activity submit
 * paths ({@code specs/057-sentence-starter-answer}). The question structure is
 * {@code {"items":[{"starter"}]}}; the student sends only continuations and the
 * server composes each sentence, so every stored sentence begins with its starter. The answer
 * row keeps the composed lines in {@code answer_text} and a snapshot
 * {@code {"starters","continuations"}} in {@code answer_json}.
 */
public final class SentenceStarters {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern TRAILING_ELLIPSIS = Pattern.compile("(\\s*(\\.\\.\\.|…))+\\s*$");
    private static final Pattern LINE_BREAKS = Pattern.compile("\\s*\\R\\s*");
    private static final String NO_SPACE_BEFORE = ",.;:!?…";

    public record Composed(String answerText, String answerJson) {}

    private SentenceStarters() {}

    /** Strips whitespace and any trailing {@code ...} / {@code …} ("porque..." → "porque"). */
    public static String normalizeStarter(String raw) {
        if (raw == null) return "";
        return TRAILING_ELLIPSIS.matcher(raw.strip()).replaceAll("").strip();
    }

    /** Validates and normalises the teacher's structure; returns the JSON to store. */
    public static String normalizeStructure(JsonNode structure) {
        JsonNode items = structure == null ? null : structure.path("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            throw new IllegalArgumentException("Añade al menos un inicio de frase.");
        }
        if (items.size() > MAX_STARTERS) {
            throw new IllegalArgumentException(
                    "Una pregunta admite como máximo " + MAX_STARTERS + " inicios de frase.");
        }
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        ArrayNode outItems = out.putArray("items");
        for (JsonNode item : items) {
            String starter = normalizeStarter(item.path("starter").asText(""));
            if (starter.isEmpty()) {
                throw new IllegalArgumentException("Cada inicio de frase necesita texto.");
            }
            if (starter.length() > MAX_STARTER_CHARS) {
                throw new IllegalArgumentException(
                        "Cada inicio de frase admite como máximo " + MAX_STARTER_CHARS + " caracteres.");
            }
            outItems.addObject().put("starter", starter);
        }
        return out.toString();
    }

    public static List<String> starters(String structureJson) {
        List<String> out = new ArrayList<>();
        for (JsonNode item : items(structureJson)) out.add(item.path("starter").asText(""));
        return out;
    }

    /** Student structure: the starters, in order. */
    public static ObjectNode studentStructure(String structureJson) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        ArrayNode arr = out.putArray("starters");
        starters(structureJson).forEach(arr::add);
        return out;
    }

    /** Starter + continuation; no space when the continuation opens with punctuation. */
    public static String compose(String starter, String continuation) {
        String c = LINE_BREAKS.matcher(continuation == null ? "" : continuation.strip()).replaceAll(" ");
        if (c.isEmpty()) return starter;
        boolean glue = NO_SPACE_BEFORE.indexOf(c.charAt(0)) >= 0;
        return starter + (glue ? "" : " ") + c;
    }

    /** Validates the continuations against the question and builds the stored answer. */
    public static Composed composeAnswer(String structureJson, List<String> continuations) {
        List<String> starters = starters(structureJson);
        List<String> given = continuations == null ? List.of() : continuations;
        if (given.size() != starters.size()) {
            throw new IllegalArgumentException("Las respuestas no coinciden con las preguntas actuales.");
        }
        List<String> lines = new ArrayList<>();
        ArrayNode stored = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < starters.size(); i++) {
            String c = given.get(i) == null ? "" : given.get(i).strip();
            if (c.isEmpty()) {
                throw new IllegalArgumentException("Debes responder a todas las preguntas.");
            }
            if (c.length() > MAX_CONTINUATION_CHARS) {
                throw new IllegalArgumentException("Una de las respuestas es demasiado larga.");
            }
            stored.add(c);
            lines.add(compose(starters.get(i), c));
        }
        String text = String.join("\n", lines);
        if (text.length() > MAX_SENTENCE_STARTER_ANSWER_CHARS) {
            throw new IllegalArgumentException("Una de las respuestas es demasiado larga.");
        }
        ObjectNode snapshot = JsonNodeFactory.instance.objectNode();
        ArrayNode s = snapshot.putArray("starters");
        starters.forEach(s::add);
        snapshot.set("continuations", stored);
        return new Composed(text, snapshot.toString());
    }

    /** Reads {@code continuations} from a submitted answer JSON; empty when absent. */
    public static List<String> continuationsFrom(JsonNode answerJson) {
        List<String> out = new ArrayList<>();
        JsonNode arr = answerJson == null ? null : answerJson.get("continuations");
        if (arr == null || !arr.isArray()) return out;
        for (JsonNode c : arr) out.add(c.isNull() ? "" : c.asText(""));
        return out;
    }

    /** Snapshot starters from a stored answer JSON; null when it is not a sentence-starter answer. */
    public static List<String> snapshotStarters(String answerJson) {
        JsonNode root = read(answerJson);
        JsonNode arr = root == null ? null : root.get("starters");
        if (arr == null || !arr.isArray()) return null;
        List<String> out = new ArrayList<>();
        for (JsonNode n : arr) out.add(n.isNull() ? null : n.asText());
        return out;
    }

    private static JsonNode items(String structureJson) {
        JsonNode root = read(structureJson);
        JsonNode items = root == null ? null : root.path("items");
        return items != null && items.isArray() ? items : JsonNodeFactory.instance.arrayNode();
    }

    private static JsonNode read(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
