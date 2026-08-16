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
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.ListeningMedia;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 *   <li>{@code MULTI_BLANK} / {@code TABLE_FILL} — each blank/cell trim + case-insensitive
 *       + accent-exact; question score = mean of unit scores.</li>
 *   <li>{@code DRAG_DROP} — each blank correct iff the placed bank item id is in
 *       that blank's {@code correctBankIds} (legacy: bank[i] for blank i).</li>
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
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final AssignmentSnapshot assignmentSnapshot;
    private final QuestionScoring questionScoring;

    public ExerciseGradingService(ContentRepository contentRepository,
                                  HomeworkQuestionRepository questionRepository,
                                  HomeworkSubmissionRepository submissionRepository,
                                  HomeworkAnswerRepository answerRepository,
                                  HomeworkTargetRepository targetRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  ObjectMapper objectMapper) {
        this.contentRepository = contentRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.assignmentSnapshot = new AssignmentSnapshot(objectMapper);
        this.questionScoring = new QuestionScoring(objectMapper);
    }

    /** Fetch an exercise to take (or re-render read-only when already graded). */
    public ExerciseResponse getExercise(String email, UUID assignmentId) {
        User user = requireUser(email);
        HomeworkAssignment assignment = requireAssigned(assignmentId, user.getId());
        notificationService.markHomeworkSeen(assignmentId, user.getId());
        if (!ListeningMedia.isComplete(assignment)) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        HomeworkComposition composition = HomeworkItems.compositionFromFormat(assignment);
        if (composition != HomeworkComposition.ALL_AUTO && composition != HomeworkComposition.MIXED) {
            // A non-exercise homework is "not found" through the exercise endpoint.
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);

        List<HomeworkQuestion> questions;
        Instant contentRevisedAt = null;
        boolean submittedWithSnapshot = existing.isPresent()
                && !HomeworkStatus.PENDING.name().equals(existing.get().getStatus())
                && assignmentSnapshot.present(existing.get());
        if (submittedWithSnapshot) {
            assignmentSnapshot.applyContent(assignment, existing.get());
            questions = assignmentSnapshot.questionsOf(existing.get());
        } else {
            questions = questionRepository.findByAssignment(assignmentId);
            if (existing.isEmpty() || HomeworkStatus.PENDING.name().equals(existing.get().getStatus())) {
                contentRevisedAt = assignment.getContentRevisedAt();
            }
        }

        ExerciseResultResponse result = null;
        String status = HomeworkStatus.PENDING.name();
        String teacherFeedback = null;
        String feedbackText = null;
        Integer scorePercent = null;
        Integer provisionalScorePercent = null;
        List<ManualAnswerViewDto> answerViews = List.of();
        if (existing.isPresent()) {
            HomeworkSubmission submission = existing.get();
            String existingStatus = submission.getStatus();
            boolean mixedAwaiting = composition == HomeworkComposition.MIXED
                    && HomeworkStatus.SUBMITTED.name().equals(existingStatus);
            boolean graded = HomeworkStatus.GRADED.name().equals(existingStatus);
            if (graded || mixedAwaiting) {
                status = existingStatus;
                result = buildStoredResult(questions, submission, mixedAwaiting);
                teacherFeedback = FormattedTextSegment.decodePlainFeedback(submission.getFeedback());
                if ("ANNOTATED".equals(submission.getReviewModel())) {
                    feedbackText = teacherFeedback;
                }
                scorePercent = graded ? submission.getScorePercent() : null;
                provisionalScorePercent = mixedAwaiting && result != null ? result.scorePercent() : null;
                if (composition == HomeworkComposition.MIXED) {
                    boolean stripTeacherScores = mixedAwaiting;
                    answerViews = answerRepository.findBySubmission(submission.getId()).stream()
                            .filter(a -> a.getPromptSnapshot() != null || a.getAnswerText() != null)
                            .map(a -> stripTeacherScores
                                    ? ManualAnswerViewDto.fromStored(
                                            a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText())
                                    : ManualAnswerViewDto.fromStored(
                                            a.getQuestionId(),
                                            a.getPromptSnapshot(),
                                            a.getAnswerText(),
                                            a.getTeacherScorePercent(),
                                            a.getScore() == null ? null : a.getScore().doubleValue()))
                            .toList();
                }
            }
        }

        return new ExerciseResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getInstructions(),
                assignment.getFormat() == null ? HomeworkFormat.EXERCISE.name() : assignment.getFormat().name(),
                composition.name(),
                status,
                assignment.getHomeworkType() == null ? null : assignment.getHomeworkType().name(),
                assignment.getAudioUrl(),
                assignment.getAudioFileId(),
                assignment.getMediaSourceKind() == null ? null : assignment.getMediaSourceKind().name(),
                buildStudentQuestions(questions),
                result,
                answerViews,
                scorePercent,
                provisionalScorePercent,
                feedbackText,
                teacherFeedback,
                contentRevisedAt,
                targetRepository.findDueOn(assignmentId, user.getId()));
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
            QuestionScoring.GradedAnswer graded = questionScoring.grade(q, given);
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

    /** Submit answers, auto-grade, persist, and return the result. Single submission only. */
    @Transactional
    public ExerciseResultResponse submit(String email, UUID assignmentId, SubmitExerciseRequest request) {
        User user = requireUser(email);
        HomeworkAssignment assignment = lockAssigned(assignmentId, user.getId());
        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);
        if (existing.isPresent() && HomeworkStatus.GRADED.name().equals(existing.get().getStatus())) {
            throw new SubmissionNotAllowedException(
                    "Este ejercicio ya ha sido entregado y no puede repetirse.", HttpStatus.CONFLICT);
        }
        assignmentSnapshot.requireCurrentRevision(assignment,
                request == null ? null : request.contentRevisedAt());
        if (!ListeningMedia.isComplete(assignment)) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        if (HomeworkItems.compositionFromFormat(assignment) != HomeworkComposition.ALL_AUTO) {
            throw new SubmissionNotAllowedException(
                    "Esta tarea no es un ejercicio autocorregible.", HttpStatus.BAD_REQUEST);
        }

        List<HomeworkQuestion> questions = questionRepository.findByAssignment(assignmentId);
        Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion = (request == null || request.answers() == null)
                ? Map.of()
                : request.answers().stream()
                    .filter(a -> a.questionId() != null)
                    .collect(Collectors.toMap(SubmitExerciseRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));

        StructuredGradeResult graded = gradeStructuredSubset(questions, byQuestion);
        int scorePercent = graded.provisionalScorePercent();

        HomeworkSubmission saved = submissionRepository.upsertGraded(
                user.getId(), assignmentId, scorePercent, Instant.now());
        answerRepository.saveAll(saved.getId(), graded.structuredAnswers());
        persistSnapshot(assignment, questions, saved);

        return graded.toProvisionalResult();
    }

    public List<ExerciseQuestionDto> studentQuestionsFor(List<HomeworkQuestion> questions) {
        return buildStudentQuestions(questions);
    }

    public ExerciseResultResponse storedResultFor(List<HomeworkQuestion> questions, HomeworkSubmission submission) {
        return buildStoredResult(questions, submission, false);
    }

    public ExerciseResultResponse storedProvisionalResultFor(
            List<HomeworkQuestion> questions, HomeworkSubmission submission) {
        return buildStoredResult(questions, submission, true);
    }

    /**
     * Reconstructs the graded result (and student-facing question list) for a
     * locked submission — used by the teacher review UI.
     */
    public record GradedExerciseView(List<ExerciseQuestionDto> questions, ExerciseResultResponse result) {}

    public GradedExerciseView viewGradedSubmission(HomeworkSubmission submission) {
        List<HomeworkQuestion> questions = questionsForResult(submission);
        return new GradedExerciseView(buildStudentQuestions(questions), buildStoredResult(questions, submission, false));
    }

    private List<HomeworkQuestion> questionsForResult(HomeworkSubmission submission) {
        if (assignmentSnapshot.present(submission)) {
            return assignmentSnapshot.questionsOf(submission);
        }
        return questionRepository.findByAssignment(submission.getAssignmentId());
    }

    private void persistSnapshot(HomeworkAssignment assignment, List<HomeworkQuestion> questions,
                                 HomeworkSubmission saved) {
        submissionRepository.setAssignmentSnapshotIfAbsent(
                saved.getId(), assignmentSnapshot.serialize(assignment, questions));
    }

    private HomeworkAssignment lockAssigned(UUID assignmentId, UUID userId) {
        HomeworkAssignment assignment = contentRepository.lockAssignment(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));
        if (!assignment.isPublished() || !targetRepository.isAssignedTo(assignmentId, userId)) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        return assignment;
    }

    // --- grading (delegates per-kind math to QuestionScoring) ----------------

    private static List<UUID> correctOptionIds(HomeworkQuestion q) {
        if (q.getKind() != QuestionKind.SINGLE_CHOICE
                && q.getKind() != QuestionKind.MULTI_CHOICE
                && q.getKind() != QuestionKind.TRUE_FALSE) {
            return List.of();
        }
        return q.getOptions().stream().filter(QuestionOption::isCorrect).map(QuestionOption::getId).toList();
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

    // --- reconstruction for a locked (already graded) exercise ------------------

    private ExerciseResultResponse buildStoredResult(List<HomeworkQuestion> questions,
                                                     HomeworkSubmission submission,
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
                    List.of(), unitResults,
                    selected));
        }
        int scorePercent;
        if (provisionalAutoOnly) {
            scorePercent = HomeworkCompositionSupport.scorePercent(scoreSum, counted);
        } else {
            scorePercent = submission.getScorePercent() == null ? 0 : submission.getScorePercent();
        }
        return new ExerciseResultResponse(scorePercent, fullyCorrect, counted, results);
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
        SubmitExerciseRequest.AnswerDto given = new SubmitExerciseRequest.AnswerDto(q.getId(), List.of(), answerJson);
        return questionScoring.grade(q, given).unitResults();
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

    /** Strips the answer key from a structured question's {@code structure_json} for student display. */
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
