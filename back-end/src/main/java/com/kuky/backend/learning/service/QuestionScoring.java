package com.kuky.backend.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionOption;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Per-kind 0–1 scoring shared by homework exercises and quizzes.
 * Does not persist; callers map {@link GradedAnswer} onto their own answer rows.
 */
public class QuestionScoring {

    private final ObjectMapper objectMapper;

    public QuestionScoring(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record GradedAnswer(
            double score,
            List<UUID> selectedOptionIds,
            String answerJson,
            List<ExerciseResultResponse.UnitResultDto> unitResults) {}

    public GradedAnswer grade(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        return switch (q.getKind()) {
            case SINGLE_CHOICE, TRUE_FALSE -> gradeSingleChoice(q, given);
            case MULTI_CHOICE -> gradeMultiChoice(q, given);
            case MULTI_BLANK -> gradeMultiBlank(q, given);
            case DRAG_DROP -> gradeDragDrop(q, given);
            case TABLE_FILL -> gradeTableFill(q, given);
            case MATCHING -> gradeMatching(q, given);
            case FREE_TEXT -> throw new IllegalStateException("FREE_TEXT no se califica automáticamente.");
        };
    }

    private GradedAnswer gradeSingleChoice(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        if (SingleChoiceItems.isNumbered(q)) {
            SingleChoiceItems.GradedNumbered numbered = SingleChoiceItems.grade(q, given);
            return new GradedAnswer(numbered.meanScore(), List.of(), numbered.answerJson(), numbered.unitResults());
        }
        Set<UUID> selected = selectedFor(q, given);
        Set<UUID> correct = q.getOptions().stream()
                .filter(QuestionOption::isCorrect).map(QuestionOption::getId)
                .collect(Collectors.toSet());
        double score = selected.equals(correct) ? 1.0 : 0.0;
        return new GradedAnswer(score, new ArrayList<>(selected), null, List.of());
    }

    private GradedAnswer gradeMultiChoice(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        int n = q.getOptions().size();
        if (n == 0) return new GradedAnswer(0.0, new ArrayList<>(selected), null, List.of());
        int rightDecisions = 0;
        for (QuestionOption o : q.getOptions()) {
            boolean isSelected = selected.contains(o.getId());
            if (o.isCorrect() && isSelected) rightDecisions++;
            else if (!o.isCorrect() && !isSelected) rightDecisions++;
        }
        double score = (double) rightDecisions / n;
        return new GradedAnswer(score, new ArrayList<>(selected), null, List.of());
    }

    private GradedAnswer gradeMultiBlank(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        JsonNode blanks = readStructure(q).path("blanks");
        int n = blanks.isArray() ? blanks.size() : 0;
        JsonNode studentBlanks = answerJsonOf(given).path("blanks");

        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            List<String> accepted = toStringList(blanks.get(i).path("acceptedAnswers"));
            String studentValue = textAt(studentBlanks, i);
            boolean correct = matchesAny(studentValue, accepted);
            sum += correct ? 1.0 : 0.0;
            List<String> expectedDisplay = expectedDisplayForMulti(accepted, correct);
            units.add(unit(i, correct, studentValue, expectedDisplay));
        }
        double score = n == 0 ? 0.0 : sum / n;
        return new GradedAnswer(score, List.of(), storedAnswerJson(given), units);
    }

    private GradedAnswer gradeDragDrop(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        JsonNode structure = readStructure(q);
        DragDropStructureSupport.Resolved resolved = DragDropStructureSupport.resolve(structure);
        JsonNode bank = resolved.bank();
        int n = resolved.blankCount();
        JsonNode placements = answerJsonOf(given).path("placements");

        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            List<String> correctIds = resolved.blanks().get(i).correctBankIds();
            String placedId = textAt(placements, i);
            boolean correct = placedId != null && correctIds.contains(placedId);
            sum += correct ? 1.0 : 0.0;
            List<String> expectedLabels = DragDropStructureSupport.labelsForIds(bank, correctIds);
            expectedLabels = expectedLabels.stream().filter(l -> l != null && !l.isBlank()).toList();
            String studentDisplay = correct
                    ? DragDropStructureSupport.labelForId(bank, placedId)
                    : labelForId(bank, placedId);
            List<String> expectedDisplay = expectedDisplayForMulti(expectedLabels, correct);
            units.add(unit(i, correct, studentDisplay, expectedDisplay));
        }
        double score = n == 0 ? 0.0 : sum / n;
        return new GradedAnswer(score, List.of(), storedAnswerJson(given), units);
    }

    private static List<String> expectedDisplayForMulti(List<String> accepted, boolean correct) {
        if (accepted == null || accepted.isEmpty()) return List.of();
        if (accepted.size() > 1) return accepted;
        return correct ? List.of() : accepted;
    }

    private GradedAnswer gradeTableFill(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        List<JsonNode> blankCells = blankCellsSorted(readStructure(q).path("cells"));
        JsonNode answerCells = answerJsonOf(given).path("cells");

        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < blankCells.size(); i++) {
            JsonNode cell = blankCells.get(i);
            List<String> accepted = toStringList(cell.path("acceptedAnswers"));
            String key = cell.path("r").asInt() + "," + cell.path("c").asInt();
            JsonNode valueNode = answerCells.get(key);
            String studentValue = valueNode != null && valueNode.isTextual() ? valueNode.asText() : null;
            boolean correct = matchesAny(studentValue, accepted);
            sum += correct ? 1.0 : 0.0;
            units.add(unit(i, correct, studentValue, correct ? List.of() : accepted));
        }
        double score = blankCells.isEmpty() ? 0.0 : sum / blankCells.size();
        return new GradedAnswer(score, List.of(), storedAnswerJson(given), units);
    }

    private GradedAnswer gradeMatching(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        JsonNode structure = readStructure(q);
        JsonNode pairs = structure.path("pairs");
        JsonNode right = structure.path("right");
        int n = pairs.isArray() ? pairs.size() : 0;

        Map<String, String> studentPairs = new HashMap<>();
        JsonNode givenPairs = answerJsonOf(given).path("pairs");
        if (givenPairs.isArray()) {
            for (JsonNode p : givenPairs) {
                String leftId = p.path("leftId").asText(null);
                String rightId = p.path("rightId").asText(null);
                if (leftId != null) studentPairs.put(leftId, rightId);
            }
        }

        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            JsonNode pair = pairs.get(i);
            String leftId = pair.path("leftId").asText(null);
            String expectedRightId = pair.path("rightId").asText(null);
            String studentRightId = studentPairs.get(leftId);
            boolean correct = expectedRightId != null && expectedRightId.equals(studentRightId);
            sum += correct ? 1.0 : 0.0;
            String expectedLabel = labelForId(right, expectedRightId);
            String studentDisplay = correct ? expectedLabel : labelForId(right, studentRightId);
            units.add(unit(i, correct, studentDisplay, correct ? List.of() : List.of(expectedLabel)));
        }
        double score = n == 0 ? 0.0 : sum / n;
        return new GradedAnswer(score, List.of(), storedAnswerJson(given), units);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesAny(String studentValue, List<String> accepted) {
        if (studentValue == null || studentValue.isBlank()) return false;
        String normalized = normalize(studentValue);
        return accepted.stream().anyMatch(a -> normalize(a).equals(normalized));
    }

    private static ExerciseResultResponse.UnitResultDto unit(
            int index, boolean correct, String studentDisplay, List<String> expectedDisplay) {
        return new ExerciseResultResponse.UnitResultDto(index, correct ? 1.0 : 0.0, correct, studentDisplay, expectedDisplay);
    }

    private Set<UUID> selectedFor(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        if (given == null || given.selectedOptionIds() == null) return Set.of();
        Set<UUID> valid = q.getOptions().stream().map(QuestionOption::getId).collect(Collectors.toSet());
        Set<UUID> selected = new HashSet<>(given.selectedOptionIds());
        selected.retainAll(valid);
        return selected;
    }

    private JsonNode readStructure(HomeworkQuestion q) {
        String json = q.getStructureJson();
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(json);
            return node == null || node.isNull() ? objectMapper.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode answerJsonOf(SubmitExerciseRequest.AnswerDto given) {
        if (given == null || given.answerJson() == null || given.answerJson().isNull()) {
            return objectMapper.createObjectNode();
        }
        return given.answerJson();
    }

    private String storedAnswerJson(SubmitExerciseRequest.AnswerDto given) {
        if (given == null || given.answerJson() == null || given.answerJson().isNull()) return null;
        try {
            return objectMapper.writeValueAsString(given.answerJson());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static List<String> toStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonNode n : arrayNode) {
            if (n.isTextual()) out.add(n.asText());
        }
        return out;
    }

    private static String textAt(JsonNode arrayNode, int index) {
        if (arrayNode == null || !arrayNode.isArray() || index >= arrayNode.size()) return null;
        JsonNode n = arrayNode.get(index);
        return n != null && n.isTextual() ? n.asText() : null;
    }

    private static String labelForId(JsonNode items, String id) {
        if (id == null || items == null || !items.isArray()) return null;
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText(null))) return item.path("label").asText(null);
        }
        return null;
    }

    private static List<JsonNode> blankCellsSorted(JsonNode cells) {
        List<JsonNode> blanks = new ArrayList<>();
        if (cells.isArray()) {
            for (JsonNode c : cells) {
                if ("blank".equals(c.path("type").asText())) blanks.add(c);
            }
        }
        blanks.sort(Comparator.comparingInt((JsonNode c) -> c.path("r").asInt())
                .thenComparingInt(c -> c.path("c").asInt()));
        return blanks;
    }
}
