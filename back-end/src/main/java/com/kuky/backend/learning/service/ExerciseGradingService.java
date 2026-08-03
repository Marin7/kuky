package com.kuky.backend.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResponse;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-grades self-correcting exercises and builds the student-facing exercise
 * view (answer key hidden until submission). Grading rules (per spec/research §5
 * and {@code specs/024-new-exercise-types/data-model.md}):
 * <ul>
 *   <li>{@code SINGLE_CHOICE} — 0/1 (selected set must equal the correct option).</li>
 *   <li>{@code MULTI_CHOICE} — partial credit over all options.</li>
 *   <li>{@code FILL_BLANK} — trim + case-insensitive + accent-exact.</li>
 *   <li>{@code MULTI_BLANK} / {@code TABLE_FILL} — each blank/cell trim + case-insensitive
 *       + accent-exact; question score = mean of unit scores.</li>
 *   <li>{@code DRAG_DROP} — each blank correct iff the placed bank item id matches the
 *       bank item at that index.</li>
 *   <li>{@code MATCHING} — each authored pair is a unit; correct iff the student paired
 *       the same leftId ↔ rightId.</li>
 * </ul>
 * New kinds store their answer key in {@code structure_json} (stripped for the
 * student-facing DTO) and the student's raw answer in {@code answer_json}.
 */
@Service
public class ExerciseGradingService {

    private final ContentRepository contentRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkAnswerRepository answerRepository;
    private final HomeworkTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ExerciseGradingService(ContentRepository contentRepository,
                                  HomeworkQuestionRepository questionRepository,
                                  HomeworkSubmissionRepository submissionRepository,
                                  HomeworkAnswerRepository answerRepository,
                                  HomeworkTargetRepository targetRepository,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper) {
        this.contentRepository = contentRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /** Fetch an exercise to take (or re-render read-only when already graded). */
    public ExerciseResponse getExercise(String email, UUID assignmentId) {
        User user = requireUser(email);
        HomeworkAssignment assignment = requireAssigned(assignmentId, user.getId());
        if (assignment.getFormat() != HomeworkFormat.EXERCISE) {
            // A non-exercise homework is "not found" through the exercise endpoint.
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        List<HomeworkQuestion> questions = questionRepository.findByAssignment(assignmentId);

        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);

        ExerciseResultResponse result = null;
        String status = HomeworkStatus.PENDING.name();
        if (existing.isPresent() && HomeworkStatus.GRADED.name().equals(existing.get().getStatus())) {
            status = HomeworkStatus.GRADED.name();
            result = buildStoredResult(questions, existing.get());
        }

        return new ExerciseResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getInstructions(),
                HomeworkFormat.EXERCISE.name(),
                status,
                assignment.getHomeworkType() == null ? null : assignment.getHomeworkType().name(),
                assignment.getAudioUrl(),
                assignment.getAudioFileId(),
                buildStudentQuestions(questions),
                result);
    }

    /** Submit answers, auto-grade, persist, and return the result. Single submission only. */
    @Transactional
    public ExerciseResultResponse submit(String email, UUID assignmentId, SubmitExerciseRequest request) {
        User user = requireUser(email);
        HomeworkAssignment assignment = requireAssigned(assignmentId, user.getId());
        if (assignment.getFormat() != HomeworkFormat.EXERCISE) {
            throw new SubmissionNotAllowedException(
                    "Esta tarea no es un ejercicio autocorregible.", HttpStatus.BAD_REQUEST);
        }

        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);
        if (existing.isPresent() && HomeworkStatus.GRADED.name().equals(existing.get().getStatus())) {
            throw new SubmissionNotAllowedException(
                    "Este ejercicio ya ha sido entregado y no puede repetirse.", HttpStatus.CONFLICT);
        }

        List<HomeworkQuestion> questions = questionRepository.findByAssignment(assignmentId);
        Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion = (request == null || request.answers() == null)
                ? Map.of()
                : request.answers().stream()
                    .filter(a -> a.questionId() != null)
                    .collect(Collectors.toMap(SubmitExerciseRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));

        List<HomeworkAnswer> answers = new ArrayList<>();
        List<ExerciseResultResponse.QuestionResultDto> questionResults = new ArrayList<>();
        double scoreSum = 0;
        int fullyCorrect = 0;

        for (HomeworkQuestion q : questions) {
            SubmitExerciseRequest.AnswerDto given = byQuestion.get(q.getId());
            GradedAnswer graded = gradeQuestion(q, given);

            scoreSum += graded.score();
            if (graded.score() >= 1.0) fullyCorrect++;

            HomeworkAnswer answer = new HomeworkAnswer();
            answer.setQuestionId(q.getId());
            answer.setAnswerText(graded.answerText());
            answer.setAnswerJson(graded.answerJson());
            answer.setScore(BigDecimal.valueOf(graded.score()).setScale(3, RoundingMode.HALF_UP));
            answer.setSelectedOptionIds(graded.selectedOptionIds());
            answers.add(answer);

            questionResults.add(new ExerciseResultResponse.QuestionResultDto(
                    q.getId(), graded.score(), graded.score() >= 1.0,
                    correctOptionIds(q), acceptedAnswers(q), graded.unitResults()));
        }

        int total = questions.size();
        int scorePercent = total == 0 ? 0 : (int) Math.round((scoreSum / total) * 100);

        HomeworkSubmission saved = submissionRepository.upsertGraded(
                user.getId(), assignmentId, scorePercent, Instant.now());
        answerRepository.saveAll(saved.getId(), answers);

        return new ExerciseResultResponse(scorePercent, fullyCorrect, total, questionResults);
    }

    // --- grading --------------------------------------------------------------

    private record GradedAnswer(
            double score,
            String answerText,
            List<UUID> selectedOptionIds,
            String answerJson,
            List<ExerciseResultResponse.UnitResultDto> unitResults) {}

    private GradedAnswer gradeQuestion(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        return switch (q.getKind()) {
            case SINGLE_CHOICE -> gradeSingleChoice(q, given);
            case MULTI_CHOICE -> gradeMultiChoice(q, given);
            case FILL_BLANK -> gradeFillBlank(q, given);
            case MULTI_BLANK -> gradeMultiBlank(q, given);
            case DRAG_DROP -> gradeDragDrop(q, given);
            case TABLE_FILL -> gradeTableFill(q, given);
            case MATCHING -> gradeMatching(q, given);
        };
    }

    private GradedAnswer gradeSingleChoice(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        Set<UUID> correct = q.getOptions().stream()
                .filter(QuestionOption::isCorrect).map(QuestionOption::getId)
                .collect(Collectors.toSet());
        double score = selected.equals(correct) ? 1.0 : 0.0;
        return new GradedAnswer(score, null, new ArrayList<>(selected), null, List.of());
    }

    private GradedAnswer gradeMultiChoice(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        int n = q.getOptions().size();
        if (n == 0) return new GradedAnswer(0.0, null, new ArrayList<>(selected), null, List.of());
        int rightDecisions = 0;
        for (QuestionOption o : q.getOptions()) {
            boolean isSelected = selected.contains(o.getId());
            if (o.isCorrect() && isSelected) rightDecisions++;
            else if (!o.isCorrect() && !isSelected) rightDecisions++;
        }
        double score = (double) rightDecisions / n;
        return new GradedAnswer(score, null, new ArrayList<>(selected), null, List.of());
    }

    private GradedAnswer gradeFillBlank(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        String raw = given == null ? null : given.answerText();
        if (raw == null || raw.isBlank()) {
            return new GradedAnswer(0.0, raw, List.of(), null, List.of());
        }
        String normalized = normalize(raw);
        boolean matches = q.getOptions().stream()
                .anyMatch(o -> normalize(o.getLabel()).equals(normalized));
        return new GradedAnswer(matches ? 1.0 : 0.0, raw, List.of(), null, List.of());
    }

    /** {@code structure.blanks[].acceptedAnswers} vs. student {@code answerJson.blanks[]}. */
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
            units.add(unit(i, correct, studentValue, correct ? List.of() : accepted));
        }
        double score = n == 0 ? 0.0 : sum / n;
        return new GradedAnswer(score, null, List.of(), storedAnswerJson(given), units);
    }

    /** {@code structure.bank[i]} is the correct placement for blank {@code i}. */
    private GradedAnswer gradeDragDrop(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        JsonNode bank = readStructure(q).path("bank");
        int n = bank.isArray() ? bank.size() : 0;
        JsonNode placements = answerJsonOf(given).path("placements");

        List<ExerciseResultResponse.UnitResultDto> units = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            JsonNode bankItem = bank.get(i);
            String expectedId = bankItem.path("id").asText(null);
            String expectedLabel = bankItem.path("label").asText(null);
            String placedId = textAt(placements, i);
            boolean correct = placedId != null && placedId.equals(expectedId);
            sum += correct ? 1.0 : 0.0;
            String studentDisplay = correct ? expectedLabel : labelForId(bank, placedId);
            units.add(unit(i, correct, studentDisplay, correct ? List.of() : List.of(expectedLabel)));
        }
        double score = n == 0 ? 0.0 : sum / n;
        return new GradedAnswer(score, null, List.of(), storedAnswerJson(given), units);
    }

    /** Blank cells graded in {@code (r,c)} order; {@code answerJson.cells} keyed by {@code "r,c"}. */
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
        return new GradedAnswer(score, null, List.of(), storedAnswerJson(given), units);
    }

    /** Each authored {@code structure.pairs[]} entry is a unit; student pairs given in {@code answerJson.pairs}. */
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
        return new GradedAnswer(score, null, List.of(), storedAnswerJson(given), units);
    }

    /** Trim + case-insensitive but accent-exact (no diacritic stripping). */
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

    /** Selected option ids restricted to options that actually belong to the question. */
    private Set<UUID> selectedFor(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        if (given == null || given.selectedOptionIds() == null) return Set.of();
        Set<UUID> valid = q.getOptions().stream().map(QuestionOption::getId).collect(Collectors.toSet());
        Set<UUID> selected = new HashSet<>(given.selectedOptionIds());
        selected.retainAll(valid);
        return selected;
    }

    private static List<UUID> correctOptionIds(HomeworkQuestion q) {
        if (q.getKind() != QuestionKind.SINGLE_CHOICE && q.getKind() != QuestionKind.MULTI_CHOICE) return List.of();
        return q.getOptions().stream().filter(QuestionOption::isCorrect).map(QuestionOption::getId).toList();
    }

    private static List<String> acceptedAnswers(HomeworkQuestion q) {
        if (q.getKind() != QuestionKind.FILL_BLANK) return List.of();
        return q.getOptions().stream().map(QuestionOption::getLabel).toList();
    }

    // --- JSON helpers -----------------------------------------------------------

    /** Parses a question's {@code structure_json}; empty object on missing/invalid JSON. */
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

    /** The student's raw {@code answerJson}, or an empty object when missing (all units then score 0). */
    private JsonNode answerJsonOf(SubmitExerciseRequest.AnswerDto given) {
        if (given == null || given.answerJson() == null || given.answerJson().isNull()) {
            return objectMapper.createObjectNode();
        }
        return given.answerJson();
    }

    /** Parses a stored {@code answer_json} string; empty object on missing/invalid JSON. */
    private JsonNode parseAnswerJson(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(json);
            return node == null || node.isNull() ? objectMapper.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    /** Serializes the student's submitted {@code answerJson} for persistence; {@code null} when absent. */
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

    /** Finds the {@code label} of the {@code {id,label}} entry matching {@code id} within {@code items}. */
    private static String labelForId(JsonNode items, String id) {
        if (id == null || items == null || !items.isArray()) return null;
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText(null))) return item.path("label").asText(null);
        }
        return null;
    }

    /** {@code TABLE_FILL} blank cells, sorted by {@code (r,c)} (the grading/display unit order). */
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

    // --- reconstruction for a locked (already graded) exercise ------------------

    private ExerciseResultResponse buildStoredResult(List<HomeworkQuestion> questions, HomeworkSubmission submission) {
        Map<UUID, HomeworkAnswer> byQuestion = answerRepository.findBySubmission(submission.getId()).stream()
                .filter(a -> a.getQuestionId() != null)
                .collect(Collectors.toMap(HomeworkAnswer::getQuestionId, Function.identity(), (a, b) -> a));

        List<ExerciseResultResponse.QuestionResultDto> results = new ArrayList<>();
        int fullyCorrect = 0;
        for (HomeworkQuestion q : questions) {
            HomeworkAnswer a = byQuestion.get(q.getId());
            double score = a == null || a.getScore() == null ? 0.0 : a.getScore().doubleValue();
            boolean correct = score >= 1.0;
            if (correct) fullyCorrect++;
            List<ExerciseResultResponse.UnitResultDto> unitResults =
                    q.getKind().isStructured() ? recomputeUnitResults(q, a) : List.of();
            results.add(new ExerciseResultResponse.QuestionResultDto(
                    q.getId(), score, correct, correctOptionIds(q), acceptedAnswers(q), unitResults));
        }
        int scorePercent = submission.getScorePercent() == null ? 0 : submission.getScorePercent();
        return new ExerciseResultResponse(scorePercent, fullyCorrect, questions.size(), results);
    }

    /**
     * Re-derives per-unit feedback for a structured kind from the stored
     * {@code answer_json} against the question's current structure — used so a
     * locked exercise still shows unit-level right/wrong on review. The overall
     * per-question score/correct flag above is intentionally taken from the
     * persisted value, not recomputed, so answer-key edits never change past grades.
     */
    private List<ExerciseResultResponse.UnitResultDto> recomputeUnitResults(HomeworkQuestion q, HomeworkAnswer a) {
        JsonNode answerJson = a == null ? null : parseAnswerJson(a.getAnswerJson());
        SubmitExerciseRequest.AnswerDto given = new SubmitExerciseRequest.AnswerDto(q.getId(), List.of(), null, answerJson);
        return gradeQuestion(q, given).unitResults();
    }

    private List<ExerciseQuestionDto> buildStudentQuestions(List<HomeworkQuestion> questions) {
        return questions.stream().map(q -> {
            boolean hasOptions = q.getKind() == QuestionKind.SINGLE_CHOICE || q.getKind() == QuestionKind.MULTI_CHOICE;
            List<ExerciseQuestionDto.StudentOptionDto> options = hasOptions
                    ? q.getOptions().stream()
                        .map(o -> new ExerciseQuestionDto.StudentOptionDto(o.getId(), o.getLabel()))
                        .toList()
                    : List.of();
            JsonNode structure = q.getKind().isStructured() ? stripStructureForStudent(q) : null;
            return new ExerciseQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(), options, structure);
        }).toList();
    }

    /** Strips the answer key from a structured question's {@code structure_json} for student display. */
    private JsonNode stripStructureForStudent(HomeworkQuestion q) {
        JsonNode structure = readStructure(q);
        ObjectNode result = objectMapper.createObjectNode();
        switch (q.getKind()) {
            case DRAG_DROP -> result.set("bank", arrayOrEmpty(structure.path("bank")));
            case TABLE_FILL -> {
                result.set("rowHeaders", arrayOrEmpty(structure.path("rowHeaders")));
                result.set("colHeaders", arrayOrEmpty(structure.path("colHeaders")));
                ArrayNode cells = objectMapper.createArrayNode();
                for (JsonNode c : structure.path("cells")) {
                    ObjectNode cell = objectMapper.createObjectNode();
                    cell.put("r", c.path("r").asInt());
                    cell.put("c", c.path("c").asInt());
                    String type = c.path("type").asText();
                    cell.put("type", type);
                    if ("fixed".equals(type)) cell.put("text", c.path("text").asText(""));
                    cells.add(cell);
                }
                result.set("cells", cells);
            }
            case MATCHING -> {
                result.set("left", arrayOrEmpty(structure.path("left")));
                result.set("right", arrayOrEmpty(structure.path("right")));
            }
            case MULTI_BLANK -> {
                // Nothing beyond the prompt itself (blanks render from ___ tokens); {} is the wire shape.
            }
            default -> { }
        }
        return result;
    }

    private JsonNode arrayOrEmpty(JsonNode node) {
        return node != null && node.isArray() ? node.deepCopy() : objectMapper.createArrayNode();
    }

    // --- guards -------------------------------------------------------------

    private HomeworkAssignment requireAssigned(UUID assignmentId, UUID userId) {
        HomeworkAssignment assignment = contentRepository.findPublishedAssignmentById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));
        if (!targetRepository.isAssignedTo(assignmentId, userId)) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        return assignment;
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }
}
