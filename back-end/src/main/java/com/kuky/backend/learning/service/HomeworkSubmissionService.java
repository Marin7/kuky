package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.ManualAnswerDto;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.ListeningMedia;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
 * Handles student homework submission: WRITE (single rich text), and unified
 * non-WRITE answers (ALL_MANUAL / ALL_AUTO / MIXED) on the {@code /answers} path.
 */
@Service
public class HomeworkSubmissionService {

    private static final int MAX_PLAIN_ANSWER_CHARS = 2000;

    private final ContentRepository contentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final HomeworkAnswerRepository answerRepository;
    private final HomeworkTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final ExerciseGradingService gradingService;
    private final SchedulingProperties props;

    public HomeworkSubmissionService(ContentRepository contentRepository,
                                     HomeworkSubmissionRepository submissionRepository,
                                     HomeworkQuestionRepository questionRepository,
                                     HomeworkAnswerRepository answerRepository,
                                     HomeworkTargetRepository targetRepository,
                                     UserRepository userRepository,
                                     ExerciseGradingService gradingService,
                                     SchedulingProperties props) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.gradingService = gradingService;
        this.props = props;
    }

    /** WRITE homework submit (or transitional ALL_MANUAL via answers on this path). */
    @Transactional
    public HomeworkItemResponse submit(String userEmail, UUID assignmentId,
                                       List<FormattedTextSegment> response,
                                       List<ManualAnswerDto> answers) {
        User user = requireUser(userEmail);
        HomeworkAssignment assignment = requirePublished(assignmentId);
        HomeworkComposition composition = compositionOf(assignment);

        if (composition == HomeworkComposition.ALL_AUTO || composition == HomeworkComposition.MIXED) {
            throw new SubmissionNotAllowedException(
                    "Este ejercicio se entrega desde su propia página.", HttpStatus.BAD_REQUEST);
        }

        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);
        rejectIfTerminal(existing);

        HomeworkSubmission saved;
        if (composition == HomeworkComposition.ALL_MANUAL) {
            if (response != null && !response.isEmpty()) {
                throw new IllegalArgumentException("Esta tarea se entrega con respuestas por pregunta.");
            }
            List<HomeworkQuestion> questions = freeTextQuestions(assignmentId);
            List<HomeworkAnswer> mapped = validateAndMapFreeTextAnswers(questions, answers);
            saved = submissionRepository.upsert(
                    user.getId(), assignmentId, HomeworkStatus.SUBMITTED.name(), null, Instant.now());
            answerRepository.saveAll(saved.getId(), mapped);
        } else {
            // WRITE
            if (answers != null && !answers.isEmpty()) {
                throw new IllegalArgumentException("Esta tarea de escritura no admite respuestas por pregunta.");
            }
            if (response != null) {
                FormattedTextSegment.validate(response);
            }
            saved = submissionRepository.upsert(
                    user.getId(),
                    assignmentId,
                    HomeworkStatus.SUBMITTED.name(),
                    FormattedTextSegment.toJson(response),
                    Instant.now());
        }

        return toItem(assignment, saved);
    }

    /**
     * Unified non-WRITE submit: ALL_AUTO → GRADED; ALL_MANUAL → SUBMITTED;
     * MIXED → SUBMITTED with auto scores + provisional %.
     */
    @Transactional
    public HomeworkItemResponse submitAnswers(String userEmail, UUID assignmentId, SubmitExerciseRequest request) {
        User user = requireUser(userEmail);
        HomeworkAssignment assignment = requireAssigned(assignmentId, user.getId());
        if (!ListeningMedia.isComplete(assignment)) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        HomeworkComposition composition = compositionOf(assignment);

        if (composition == HomeworkComposition.WRITE) {
            throw new SubmissionNotAllowedException(
                    "Esta tarea de escritura no se entrega por preguntas.", HttpStatus.BAD_REQUEST);
        }

        Optional<HomeworkSubmission> existing =
                submissionRepository.findByUserAndAssignment(user.getId(), assignmentId);
        rejectIfTerminal(existing);

        List<HomeworkQuestion> questions = questionRepository.findByAssignment(assignmentId);
        Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion = indexAnswers(request);

        List<HomeworkAnswer> allAnswers = new ArrayList<>();
        ExerciseResultResponse autoResult = null;

        if (composition == HomeworkComposition.ALL_AUTO || composition == HomeworkComposition.MIXED) {
            requireEveryQuestionAnswered(questions, byQuestion);
            ExerciseGradingService.StructuredGradeResult graded =
                    gradingService.gradeStructuredSubset(questions, byQuestion);
            allAnswers.addAll(graded.structuredAnswers());
            autoResult = graded.toProvisionalResult();

            if (composition == HomeworkComposition.ALL_AUTO) {
                HomeworkSubmission saved = submissionRepository.upsertGraded(
                        user.getId(), assignmentId, graded.provisionalScorePercent(), Instant.now());
                answerRepository.saveAll(saved.getId(), allAnswers);
                return toItem(assignment, saved, questions, allAnswers,
                        gradingService.studentQuestionsFor(questions), autoResult);
            }

            // MIXED: also store FREE_TEXT answers
            allAnswers.addAll(mapFreeTextFromUnified(questions, byQuestion));
            HomeworkSubmission saved = submissionRepository.upsert(
                    user.getId(), assignmentId, HomeworkStatus.SUBMITTED.name(), null, Instant.now());
            answerRepository.saveAll(saved.getId(), allAnswers);
            return toItem(assignment, saved, questions, allAnswers,
                    gradingService.studentQuestionsFor(questions), autoResult);
        }

        // ALL_MANUAL via /answers
        List<ManualAnswerDto> manual = toManualDtos(byQuestion);
        List<HomeworkAnswer> mapped = validateAndMapFreeTextAnswers(
                questions.stream().filter(q -> q.getKind() == QuestionKind.FREE_TEXT).toList(), manual);
        HomeworkSubmission saved = submissionRepository.upsert(
                user.getId(), assignmentId, HomeworkStatus.SUBMITTED.name(), null, Instant.now());
        answerRepository.saveAll(saved.getId(), mapped);
        return toItem(assignment, saved, questions, mapped,
                gradingService.studentQuestionsFor(questions), null);
    }

    HomeworkItemResponse toItem(HomeworkAssignment assignment, HomeworkSubmission submission) {
        List<HomeworkQuestion> questions = assignment.getHomeworkType() == HomeworkType.WRITE
                ? List.of()
                : questionRepository.findByAssignment(assignment.getId());
        List<HomeworkAnswer> answers = submission == null
                ? List.of()
                : answerRepository.findBySubmission(submission.getId());
        HomeworkComposition composition = HomeworkCompositionSupport.compositionFromQuestions(
                assignment.getHomeworkType(),
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        List<ExerciseQuestionDto> studentQuestions = composition == HomeworkComposition.WRITE
                ? List.of()
                : gradingService.studentQuestionsFor(questions);
        ExerciseResultResponse result = null;
        if (submission != null && !answers.isEmpty()
                && (composition == HomeworkComposition.ALL_AUTO || composition == HomeworkComposition.MIXED)
                && (HomeworkStatus.GRADED.name().equals(submission.getStatus())
                || HomeworkStatus.SUBMITTED.name().equals(submission.getStatus()))) {
            result = composition == HomeworkComposition.MIXED
                    && HomeworkStatus.SUBMITTED.name().equals(submission.getStatus())
                    ? gradingService.storedProvisionalResultFor(questions, submission)
                    : gradingService.storedResultFor(questions, submission);
        }
        return toItem(assignment, submission, questions, answers, studentQuestions, result);
    }

    private HomeworkItemResponse toItem(HomeworkAssignment assignment, HomeworkSubmission submission,
                                        List<HomeworkQuestion> questions, List<HomeworkAnswer> answers,
                                        List<ExerciseQuestionDto> studentQuestions,
                                        ExerciseResultResponse result) {
        LocalDate today = LocalDate.now(ZoneId.of(props.getScheduling().getTeacherTimezone()));
        return HomeworkItems.toResponse(assignment, submission, today, null, null,
                questions, answers, studentQuestions, result);
    }

    private HomeworkComposition compositionOf(HomeworkAssignment assignment) {
        if (assignment.getHomeworkType() == HomeworkType.WRITE) {
            return HomeworkComposition.WRITE;
        }
        List<HomeworkQuestion> questions = questionRepository.findByAssignment(assignment.getId());
        return HomeworkCompositionSupport.compositionFromQuestions(
                assignment.getHomeworkType(),
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
    }

    private List<HomeworkQuestion> freeTextQuestions(UUID assignmentId) {
        return questionRepository.findByAssignment(assignmentId).stream()
                .filter(q -> q.getKind() == QuestionKind.FREE_TEXT)
                .toList();
    }

    private static Map<UUID, SubmitExerciseRequest.AnswerDto> indexAnswers(SubmitExerciseRequest request) {
        if (request == null || request.answers() == null) return Map.of();
        return request.answers().stream()
                .filter(a -> a.questionId() != null)
                .collect(Collectors.toMap(SubmitExerciseRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));
    }

    private static void requireEveryQuestionAnswered(
            List<HomeworkQuestion> questions, Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion) {
        for (HomeworkQuestion q : questions) {
            SubmitExerciseRequest.AnswerDto given = byQuestion.get(q.getId());
            if (given == null) {
                throw new IllegalArgumentException("Debes responder a todas las preguntas.");
            }
            if (q.getKind() == QuestionKind.FREE_TEXT) {
                String text = given.text() == null ? "" : given.text().strip();
                if (text.isEmpty()) {
                    throw new IllegalArgumentException("Debes responder a todas las preguntas.");
                }
                if (text.length() > MAX_PLAIN_ANSWER_CHARS) {
                    throw new IllegalArgumentException("Una de las respuestas es demasiado larga.");
                }
            }
        }
        if (!byQuestion.keySet().equals(questions.stream().map(HomeworkQuestion::getId).collect(Collectors.toSet()))) {
            throw new IllegalArgumentException("Las respuestas no coinciden con las preguntas actuales.");
        }
    }

    private static List<HomeworkAnswer> mapFreeTextFromUnified(
            List<HomeworkQuestion> questions, Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion) {
        List<HomeworkAnswer> mapped = new ArrayList<>();
        for (HomeworkQuestion q : questions) {
            if (q.getKind() != QuestionKind.FREE_TEXT) continue;
            SubmitExerciseRequest.AnswerDto given = byQuestion.get(q.getId());
            String text = given.text().strip();
            HomeworkAnswer row = new HomeworkAnswer();
            row.setQuestionId(q.getId());
            row.setAnswerText(text);
            row.setPromptSnapshot(q.getPrompt());
            row.setScore(BigDecimal.ZERO);
            row.setSelectedOptionIds(List.of());
            mapped.add(row);
        }
        return mapped;
    }

    private static List<ManualAnswerDto> toManualDtos(Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion) {
        List<ManualAnswerDto> out = new ArrayList<>();
        for (var e : byQuestion.entrySet()) {
            out.add(new ManualAnswerDto(e.getKey(), e.getValue().text()));
        }
        return out;
    }

    /** Once delivered (SUBMITTED / REVIEWED / GRADED), the student cannot change answers. */
    private static void rejectIfTerminal(Optional<HomeworkSubmission> existing) {
        if (existing.isEmpty()) {
            return;
        }
        String status = existing.get().getStatus();
        if (HomeworkStatus.REVIEWED.name().equals(status)) {
            throw new SubmissionNotAllowedException(
                    "Esta tarea ya ha sido revisada y no puede modificarse.");
        }
        if (HomeworkStatus.GRADED.name().equals(status)) {
            throw new SubmissionNotAllowedException(
                    "Este ejercicio ya ha sido entregado y no puede repetirse.");
        }
        if (HomeworkStatus.SUBMITTED.name().equals(status)) {
            throw new SubmissionNotAllowedException(
                    "Esta tarea ya ha sido entregada y no puede modificarse.");
        }
    }

    private HomeworkAssignment requirePublished(UUID assignmentId) {
        return contentRepository.findPublishedAssignmentById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));
    }

    private HomeworkAssignment requireAssigned(UUID assignmentId, UUID userId) {
        HomeworkAssignment assignment = requirePublished(assignmentId);
        if (!targetRepository.isAssignedTo(assignmentId, userId)) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        return assignment;
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    static List<HomeworkAnswer> validateAndMapFreeTextAnswers(List<HomeworkQuestion> questions,
                                                              List<ManualAnswerDto> answers) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Esta tarea no tiene preguntas configuradas.");
        }
        List<ManualAnswerDto> given = answers == null ? List.of() : answers;
        Map<UUID, String> byQuestion = new HashMap<>();
        for (ManualAnswerDto a : given) {
            if (a == null || a.questionId() == null) {
                throw new IllegalArgumentException("Cada respuesta necesita una pregunta.");
            }
            if (byQuestion.containsKey(a.questionId())) {
                throw new IllegalArgumentException("Hay respuestas duplicadas para la misma pregunta.");
            }
            String text = a.text() == null ? "" : a.text().strip();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Debes responder a todas las preguntas.");
            }
            if (text.length() > MAX_PLAIN_ANSWER_CHARS) {
                throw new IllegalArgumentException("Una de las respuestas es demasiado larga.");
            }
            byQuestion.put(a.questionId(), text);
        }

        Set<UUID> expected = new HashSet<>();
        List<HomeworkAnswer> mapped = new ArrayList<>();
        for (HomeworkQuestion q : questions) {
            expected.add(q.getId());
            String text = byQuestion.get(q.getId());
            if (text == null) {
                throw new IllegalArgumentException("Debes responder a todas las preguntas.");
            }
            HomeworkAnswer row = new HomeworkAnswer();
            row.setQuestionId(q.getId());
            row.setAnswerText(text);
            row.setPromptSnapshot(q.getPrompt());
            row.setScore(BigDecimal.ZERO);
            row.setSelectedOptionIds(List.of());
            mapped.add(row);
        }
        if (!byQuestion.keySet().equals(expected)) {
            throw new IllegalArgumentException("Las respuestas no coinciden con las preguntas actuales.");
        }
        return mapped;
    }
}
