package com.kuky.backend.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.model.HomeworkQuestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPOT_WRONG_WORDS helpers shared by authoring validation and both graders
 * ({@code specs/054-spot-wrong-words}). The passage is the question prompt; the
 * answer key is {@code {"errors":[{"wordIndex","word","correction"}]}}; the student
 * answer is {@code {"selected":[wordIndex…]}}.
 */
public final class SpotWrongWords {

    /** Unit label marking a correct word the student selected. */
    public static final String EXTRA_LABEL = "EXTRA";

    /**
     * One word = letters/marks/digits, optionally joined by an internal apostrophe or hyphen.
     * MUST stay identical to the regex in {@code front-end/src/lib/spotWrongWords.ts}.
     */
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+(?:['’\\-][\\p{L}\\p{M}\\p{N}]+)*");

    private SpotWrongWords() {}

    public static List<String> tokenize(String text) {
        List<String> words = new ArrayList<>();
        if (text == null) return words;
        Matcher m = WORD.matcher(text);
        while (m.find()) words.add(m.group());
        return words;
    }

    public static int errorCount(JsonNode structure) {
        JsonNode errors = structure == null ? null : structure.path("errors");
        return errors != null && errors.isArray() ? errors.size() : 0;
    }

    /**
     * Scores found ÷ errors. Out-of-range, duplicate or non-integer selections are dropped;
     * more remaining selections than errors is refused (the cap, FR-008a).
     */
    public static QuestionScoring.GradedAnswer grade(HomeworkQuestion q, JsonNode answerJson, ObjectMapper om) {
        JsonNode structure = readStructure(q, om);
        List<String> words = tokenize(q.getPrompt());
        int wordCount = words.size();

        Map<Integer, JsonNode> errorsByIndex = new HashMap<>();
        for (JsonNode e : structure.path("errors")) {
            errorsByIndex.put(e.path("wordIndex").asInt(-1), e);
        }
        int errorCount = errorsByIndex.size();

        TreeSet<Integer> selected = new TreeSet<>();
        JsonNode given = answerJson == null ? null : answerJson.path("selected");
        if (given != null && given.isArray()) {
            for (JsonNode n : given) {
                if (n.isIntegralNumber() && n.asInt() >= 0 && n.asInt() < wordCount) selected.add(n.asInt());
            }
        }
        if (selected.size() > errorCount) {
            throw new IllegalArgumentException("Has marcado más palabras que errores.");
        }

        TreeSet<Integer> unitIndices = new TreeSet<>(errorsByIndex.keySet());
        unitIndices.addAll(selected);
        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        int found = 0;
        for (int index : unitIndices) {
            JsonNode error = errorsByIndex.get(index);
            boolean isSelected = selected.contains(index);
            if (error == null) {
                units.add(new ExerciseResultResponse.UnitResultDto(
                        index, 0.0, false, words.get(index), List.of(), EXTRA_LABEL));
                continue;
            }
            String word = error.path("word").asText(index < words.size() ? words.get(index) : "");
            JsonNode correction = error.get("correction");
            List<String> expected = correction != null && correction.isTextual() && !correction.asText().isBlank()
                    ? List.of(correction.asText()) : List.of();
            if (isSelected) found++;
            units.add(new ExerciseResultResponse.UnitResultDto(
                    index, isSelected ? 1.0 : 0.0, isSelected, isSelected ? word : null, expected, null));
        }

        double score = errorCount == 0 ? 0.0 : (double) found / errorCount;
        ObjectNode stored = om.createObjectNode();
        ArrayNode storedSelected = stored.putArray("selected");
        selected.forEach(storedSelected::add);
        return new QuestionScoring.GradedAnswer(score, List.of(), stored.toString(), units);
    }

    private static JsonNode readStructure(HomeworkQuestion q, ObjectMapper om) {
        String json = q.getStructureJson();
        if (json == null || json.isBlank()) return om.createObjectNode();
        try {
            JsonNode node = om.readTree(json);
            return node == null || node.isNull() ? om.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            return om.createObjectNode();
        }
    }
}
