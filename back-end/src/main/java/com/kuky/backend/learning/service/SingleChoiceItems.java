package com.kuky.backend.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Numbered opción única: options live in {@code structure_json.items}, answers in
 * {@code answer_json.selections}. Classic (no {@code (N)} markers) is unchanged.
 */
public final class SingleChoiceItems {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SingleChoiceItems() {}

    public static boolean isNumbered(HomeworkQuestion q) {
        return q != null
                && q.getKind() == QuestionKind.SINGLE_CHOICE
                && SingleChoiceMarkerParser.hasMarkers(q.getPrompt());
    }

    public static int itemCount(HomeworkQuestion q) {
        if (!isNumbered(q)) return 0;
        SingleChoiceMarkerParser.ParseResult parsed = SingleChoiceMarkerParser.parse(q.getPrompt());
        return parsed.valid() ? parsed.n() : 0;
    }

    public static void requireCompleteSelections(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        if (!isNumbered(q)) return;
        if (!hasCompleteSelections(q, given)) {
            throw new IllegalArgumentException("Debes responder a todas las preguntas.");
        }
    }

    public static boolean hasCompleteSelections(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        int n = itemCount(q);
        if (n < 1) return true;
        JsonNode selections = selectionsOf(given);
        for (int i = 1; i <= n; i++) {
            String id = textId(selections, i);
            if (id == null || id.isBlank()) return false;
        }
        return true;
    }

    /**
     * Per-item 0/1 vs the answer key. Missing/unknown selection → incorrect (does not throw).
     */
    public static GradedNumbered grade(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        int n = itemCount(q);
        JsonNode items = readStructure(q).path("items");
        JsonNode selections = selectionsOf(given);
        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        double sum = 0;
        ObjectNode storedSelections = MAPPER.createObjectNode();
        for (int i = 1; i <= n; i++) {
            JsonNode item = findItem(items, i);
            String selectedId = textId(selections, i);
            String correctId = correctOptionId(item);
            String studentLabel = optionLabel(item, selectedId);
            String expectedLabel = optionLabel(item, correctId);
            boolean correct = selectedId != null && selectedId.equals(correctId);
            if (selectedId != null && !selectedId.isBlank()) {
                storedSelections.put(String.valueOf(i), selectedId);
            }
            sum += correct ? 1.0 : 0.0;
            List<String> expected = correct || expectedLabel == null || expectedLabel.isBlank()
                    ? List.of()
                    : List.of(expectedLabel);
            units.add(new ExerciseResultResponse.UnitResultDto(
                    i - 1, correct ? 1.0 : 0.0, correct, studentLabel, expected));
        }
        double mean = n == 0 ? 0.0 : sum / n;
        ObjectNode answer = MAPPER.createObjectNode();
        answer.set("selections", storedSelections);
        return new GradedNumbered(mean, writeJson(answer), units);
    }

    /** Student-facing items: {@code {number, options:[{id,label}]}} — no {@code correct}. */
    public static JsonNode stripForStudent(HomeworkQuestion q) {
        ObjectNode result = MAPPER.createObjectNode();
        ArrayNode outItems = MAPPER.createArrayNode();
        JsonNode items = readStructure(q).path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                ObjectNode o = MAPPER.createObjectNode();
                o.put("number", item.path("number").asInt());
                ArrayNode opts = MAPPER.createArrayNode();
                for (JsonNode opt : item.path("options")) {
                    ObjectNode so = MAPPER.createObjectNode();
                    so.put("id", opt.path("id").asText(""));
                    so.put("label", opt.path("label").asText(""));
                    opts.add(so);
                }
                o.set("options", opts);
                outItems.add(o);
            }
        }
        result.set("items", outItems);
        return result;
    }

    /** N contributions of 0 or 1 from the stored selections vs the current key. */
    public static List<BigDecimal> itemContributions(HomeworkQuestion q, HomeworkAnswer answer) {
        JsonNode json = parseJson(answer == null ? null : answer.getAnswerJson());
        SubmitExerciseRequest.AnswerDto given =
                new SubmitExerciseRequest.AnswerDto(q.getId(), List.of(), json);
        GradedNumbered graded = grade(q, given);
        List<BigDecimal> out = new ArrayList<>();
        for (ExerciseResultResponse.UnitResultDto u : graded.unitResults()) {
            out.add(HomeworkCompositionSupport.scoreAsDecimal(u.score()));
        }
        if (out.isEmpty()) {
            int n = itemCount(q);
            for (int i = 0; i < n; i++) {
                out.add(HomeworkCompositionSupport.scoreAsDecimal(0.0));
            }
        }
        return out;
    }

    public record GradedNumbered(
            double meanScore,
            String answerJson,
            List<ExerciseResultResponse.UnitResultDto> unitResults
    ) {}

    private static JsonNode selectionsOf(SubmitExerciseRequest.AnswerDto given) {
        if (given == null || given.answerJson() == null || given.answerJson().isNull()) {
            return MAPPER.createObjectNode();
        }
        JsonNode selections = given.answerJson().path("selections");
        return selections.isObject() ? selections : MAPPER.createObjectNode();
    }

    private static String textId(JsonNode selections, int number) {
        JsonNode node = selections.get(String.valueOf(number));
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) {
            String t = node.asText();
            return t == null || t.isBlank() ? null : t.strip();
        }
        if (node.isNumber()) return node.asText();
        return null;
    }

    private static JsonNode findItem(JsonNode items, int number) {
        if (!items.isArray()) return MAPPER.createObjectNode();
        for (JsonNode item : items) {
            if (item.path("number").asInt() == number) return item;
        }
        return MAPPER.createObjectNode();
    }

    private static String correctOptionId(JsonNode item) {
        for (JsonNode opt : item.path("options")) {
            if (opt.path("correct").asBoolean(false)) {
                String id = opt.path("id").asText(null);
                return id == null || id.isBlank() ? null : id;
            }
        }
        return null;
    }

    private static String optionLabel(JsonNode item, String optionId) {
        if (optionId == null || optionId.isBlank()) return null;
        for (JsonNode opt : item.path("options")) {
            if (optionId.equals(opt.path("id").asText(null))) {
                String label = opt.path("label").asText(null);
                return label == null || label.isBlank() ? null : label;
            }
        }
        return null;
    }

    private static JsonNode readStructure(HomeworkQuestion q) {
        return parseJson(q == null ? null : q.getStructureJson());
    }

    private static JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return MAPPER.createObjectNode();
        try {
            JsonNode node = MAPPER.readTree(json);
            return node == null || node.isNull() ? MAPPER.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            return MAPPER.createObjectNode();
        }
    }

    private static String writeJson(JsonNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "{\"selections\":{}}";
        }
    }
}
