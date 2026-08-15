package com.kuky.backend.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.ActivityAlreadySubmittedException;
import com.kuky.backend.learning.exception.ActivityNotFoundException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivityQuestion;
import com.kuky.backend.learning.model.ActivitySubmission;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.ActivityAnswerRepository;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

/** Auto-grades EXERCISE / MIXED activities; mirrors {@link ExerciseGradingService}. */
@Service
public class ActivityExerciseGradingService {

    private final ActivityRepository activityRepository;
    private final ActivityQuestionRepository questionRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityAnswerRepository answerRepository;
    private final PresentationRepository presentationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ActivityExerciseGradingService(ActivityRepository activityRepository,
                                          ActivityQuestionRepository questionRepository,
                                          ActivitySubmissionRepository submissionRepository,
                                          ActivityAnswerRepository answerRepository,
                                          PresentationRepository presentationRepository,
                                          UserRepository userRepository,
                                          ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.presentationRepository = presentationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Grades only auto-correctible questions; FREE_TEXT entries are ignored.
     * Returns persisted answer rows + result DTOs for the structured subset.
     */
    public record StructuredGradeResult(
            List<HomeworkAnswer> structuredAnswers,
            List<ExerciseResultResponse.QuestionResultDto> questionResults,
            double scoreSum,
            int fullyCorrect,
            int structuredCount
    ) {
        public int provisionalScorePercent() {
            return HomeworkCompositionSupport.scorePercent(scoreSum, structuredCount);
        }

        public ExerciseResultResponse toProvisionalResult() {
            return new ExerciseResultResponse(
                    provisionalScorePercent(), fullyCorrect, structuredCount, questionResults);
        }
    }

    public StructuredGradeResult gradeStructuredSubset(
            List<HomeworkQuestion> questions,
            Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion) {
        List<HomeworkAnswer> answers = new ArrayList<>();
        List<ExerciseResultResponse.QuestionResultDto> questionResults = new ArrayList<>();
        double scoreSum = 0;
        int fullyCorrect = 0;
        int structuredCount = 0;

        for (HomeworkQuestion q : questions) {
            if (!HomeworkCompositionSupport.isAutoGradable(q.getKind())) continue;
            SubmitExerciseRequest.AnswerDto given = byQuestion.get(q.getId());
            SingleChoiceItems.requireCompleteSelections(q, given);
            GradedAnswer graded = gradeQuestion(q, given);
            List<Double> unitScores = graded.unitResults().stream()
                    .map(ExerciseResultResponse.UnitResultDto::score)
                    .toList();
            for (var contrib : HomeworkCompositionSupport.autoContributions(q, graded.score(), unitScores)) {
                structuredCount++;
                scoreSum += contrib.doubleValue();
                if (HomeworkCompositionSupport.isFullyCorrect(contrib)) fullyCorrect++;
            }

            HomeworkAnswer answer = new HomeworkAnswer();
            answer.setQuestionId(q.getId());
            answer.setAnswerJson(graded.answerJson());
            answer.setScore(HomeworkCompositionSupport.scoreAsDecimal(graded.score()));
            answer.setSelectedOptionIds(graded.selectedOptionIds());
            answers.add(answer);

            questionResults.add(new ExerciseResultResponse.QuestionResultDto(
                    q.getId(), graded.score(), graded.score() >= 1.0,
                    SingleChoiceItems.isNumbered(q) ? List.of() : correctOptionIds(q),
                    List.of(), graded.unitResults(),
                    graded.selectedOptionIds()));
        }
        return new StructuredGradeResult(answers, questionResults, scoreSum, fullyCorrect, structuredCount);
    }

    /** ALL_AUTO only — single submission → GRADED. Prefer {@code ActivityStudentService.submitAnswers}. */
    @Transactional
    public ExerciseResultResponse submit(String email, UUID activityId, SubmitExerciseRequest request) {
        User user = requireUser(email);
        requireAccessible(activityId, user.getId());
        List<HomeworkQuestion> questions = toHomeworkQuestions(activityId);
        HomeworkComposition composition = HomeworkCompositionSupport.activityComposition(
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        if (composition != HomeworkComposition.ALL_AUTO) {
            throw new ActivityValidationException("Esta actividad no es un ejercicio autocorregible.");
        }
        Optional<ActivitySubmission> existing =
                submissionRepository.findByUserAndActivity(user.getId(), activityId);
        if (existing.isPresent() && HomeworkStatus.GRADED.name().equals(existing.get().getStatus())) {
            throw new ActivityAlreadySubmittedException(
                    "Este ejercicio ya ha sido entregado y no puede repetirse.");
        }

        Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion = (request == null || request.answers() == null)
                ? Map.of()
                : request.answers().stream()
                    .filter(a -> a.questionId() != null)
                    .collect(Collectors.toMap(SubmitExerciseRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));

        StructuredGradeResult graded = gradeStructuredSubset(questions, byQuestion);
        int scorePercent = graded.provisionalScorePercent();
        ActivitySubmission saved = submissionRepository.upsertGraded(
                user.getId(), activityId, scorePercent, Instant.now());
        answerRepository.saveAll(saved.getId(), graded.structuredAnswers());
        return graded.toProvisionalResult();
    }

    public record GradedExerciseView(List<ExerciseQuestionDto> questions, ExerciseResultResponse result) {}

    public GradedExerciseView viewGradedSubmission(ActivitySubmission submission) {
        List<HomeworkQuestion> questions = toHomeworkQuestions(submission.getActivityId());
        return new GradedExerciseView(buildStudentQuestions(questions), buildStoredResult(questions, submission, false));
    }

    public List<ExerciseQuestionDto> studentQuestionsFor(UUID activityId) {
        return studentQuestionsFor(toHomeworkQuestions(activityId));
    }

    public List<ExerciseQuestionDto> studentQuestionsFor(List<HomeworkQuestion> questions) {
        return buildStudentQuestions(questions);
    }

    public ExerciseResultResponse storedResultFor(ActivitySubmission submission) {
        return storedResultFor(toHomeworkQuestions(submission.getActivityId()), submission);
    }

    public ExerciseResultResponse storedResultFor(List<HomeworkQuestion> questions, ActivitySubmission submission) {
        return buildStoredResult(questions, submission, false);
    }

    public ExerciseResultResponse storedProvisionalResultFor(
            List<HomeworkQuestion> questions, ActivitySubmission submission) {
        return buildStoredResult(questions, submission, true);
    }

    public List<HomeworkQuestion> toHomeworkQuestions(UUID activityId) {
        return questionRepository.findByActivityId(activityId).stream()
                .map(ActivityQuestion::toHomeworkQuestion).toList();
    }

    private Activity requireAccessible(UUID activityId, UUID userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("Actividad no encontrada."));
        if (!presentationRepository.isSharedWith(activity.getPresentationId(), userId)) {
            throw new ActivityNotFoundException("Actividad no encontrada.");
        }
        return activity;
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private ExerciseResultResponse buildStoredResult(List<HomeworkQuestion> questions,
                                                     ActivitySubmission submission,
                                                     boolean provisionalAutoOnly) {
        Map<UUID, HomeworkAnswer> byQuestion = answerRepository.findBySubmission(submission.getId()).stream()
                .filter(a -> a.getQuestionId() != null)
                .collect(Collectors.toMap(HomeworkAnswer::getQuestionId, Function.identity(), (a, b) -> a));

        List<ExerciseResultResponse.QuestionResultDto> results = new ArrayList<>();
        int fullyCorrect = 0;
        double scoreSum = 0;
        int counted = 0;
        for (HomeworkQuestion q : questions) {
            if (provisionalAutoOnly && !HomeworkCompositionSupport.isAutoGradable(q.getKind())) {
                continue;
            }
            HomeworkAnswer a = byQuestion.get(q.getId());
            boolean numbered = SingleChoiceItems.isNumbered(q);
            List<BigDecimal> contribs = HomeworkCompositionSupport.contributions(q, a);
            for (BigDecimal c : contribs) {
                counted++;
                scoreSum += c.doubleValue();
                if (HomeworkCompositionSupport.isFullyCorrect(c)) fullyCorrect++;
            }
            double score = a == null || a.getScore() == null ? 0.0 : a.getScore().doubleValue();
            boolean correct = score >= 1.0;
            List<ExerciseResultResponse.UnitResultDto> unitResults =
                    numbered || q.getKind().isStructured() ? recomputeUnitResults(q, a) : List.of();
            List<UUID> selected = a == null ? List.of() : a.getSelectedOptionIds();
            results.add(new ExerciseResultResponse.QuestionResultDto(
                    q.getId(), score, correct,
                    numbered ? List.of() : correctOptionIds(q),
                    List.of(), unitResults, selected));
        }
        int scorePercent;
        if (provisionalAutoOnly) {
            scorePercent = HomeworkCompositionSupport.scorePercent(scoreSum, counted);
        } else {
            scorePercent = submission.getScorePercent() == null ? 0 : submission.getScorePercent();
        }
        return new ExerciseResultResponse(scorePercent, fullyCorrect, counted, results);
    }

    private record GradedAnswer(
            double score,
            List<UUID> selectedOptionIds,
            String answerJson,
            List<ExerciseResultResponse.UnitResultDto> unitResults) {}

    private GradedAnswer gradeQuestion(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
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
            units.add(unit(i, correct, studentValue, expectedDisplayForMulti(accepted, correct)));
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
            List<String> expectedLabels = DragDropStructureSupport.labelsForIds(bank, correctIds).stream()
                    .filter(l -> l != null && !l.isBlank())
                    .toList();
            String studentDisplay = correct
                    ? DragDropStructureSupport.labelForId(bank, placedId)
                    : labelForId(bank, placedId);
            units.add(unit(i, correct, studentDisplay, expectedDisplayForMulti(expectedLabels, correct)));
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

    private static List<UUID> correctOptionIds(HomeworkQuestion q) {
        if (q.getKind() != QuestionKind.SINGLE_CHOICE
                && q.getKind() != QuestionKind.MULTI_CHOICE
                && q.getKind() != QuestionKind.TRUE_FALSE) {
            return List.of();
        }
        return q.getOptions().stream().filter(QuestionOption::isCorrect).map(QuestionOption::getId).toList();
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

    private JsonNode parseAnswerJson(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(json);
            return node == null || node.isNull() ? objectMapper.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
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

    private List<ExerciseResultResponse.UnitResultDto> recomputeUnitResults(HomeworkQuestion q, HomeworkAnswer a) {
        JsonNode answerJson = a == null ? null : parseAnswerJson(a.getAnswerJson());
        SubmitExerciseRequest.AnswerDto given = new SubmitExerciseRequest.AnswerDto(q.getId(), List.of(), answerJson);
        return gradeQuestion(q, given).unitResults();
    }

    private List<ExerciseQuestionDto> buildStudentQuestions(List<HomeworkQuestion> questions) {
        return questions.stream().map(q -> {
            boolean numbered = SingleChoiceItems.isNumbered(q);
            boolean hasOptions = !numbered && (q.getKind() == QuestionKind.SINGLE_CHOICE
                    || q.getKind() == QuestionKind.MULTI_CHOICE
                    || q.getKind() == QuestionKind.TRUE_FALSE);
            List<ExerciseQuestionDto.StudentOptionDto> options = hasOptions
                    ? q.getOptions().stream()
                        .map(o -> new ExerciseQuestionDto.StudentOptionDto(o.getId(), o.getLabel()))
                        .toList()
                    : List.of();
            JsonNode structure;
            if (numbered) {
                structure = SingleChoiceItems.stripForStudent(q);
            } else if (q.getKind().isStructured()) {
                structure = stripStructureForStudent(q);
            } else {
                structure = null;
            }
            return new ExerciseQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(), options, structure);
        }).toList();
    }

    private JsonNode stripStructureForStudent(HomeworkQuestion q) {
        JsonNode structure = readStructure(q);
        ObjectNode result = objectMapper.createObjectNode();
        switch (q.getKind()) {
            case DRAG_DROP -> {
                result.set("bank", arrayOrEmpty(structure.path("bank")));
                result.put("bankReusable", DragDropStructureSupport.isBankReusable(
                        DragDropStructureSupport.resolve(structure)));
            }
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
            case MULTI_BLANK -> { }
            default -> { }
        }
        return result;
    }

    private JsonNode arrayOrEmpty(JsonNode node) {
        return node != null && node.isArray() ? node.deepCopy() : objectMapper.createArrayNode();
    }
}
