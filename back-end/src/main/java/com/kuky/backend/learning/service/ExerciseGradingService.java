package com.kuky.backend.learning.service;

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
 * view (answer key hidden until submission). Grading rules (per spec/research §5):
 * single-choice 0/1, multi-choice partial credit, fill-blank trim +
 * case-insensitive + accent-exact.
 */
@Service
public class ExerciseGradingService {

    private final ContentRepository contentRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkAnswerRepository answerRepository;
    private final HomeworkTargetRepository targetRepository;
    private final UserRepository userRepository;

    public ExerciseGradingService(ContentRepository contentRepository,
                                  HomeworkQuestionRepository questionRepository,
                                  HomeworkSubmissionRepository submissionRepository,
                                  HomeworkAnswerRepository answerRepository,
                                  HomeworkTargetRepository targetRepository,
                                  UserRepository userRepository) {
        this.contentRepository = contentRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
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

            scoreSum += graded.score;
            if (graded.score >= 1.0) fullyCorrect++;

            HomeworkAnswer answer = new HomeworkAnswer();
            answer.setQuestionId(q.getId());
            answer.setAnswerText(graded.answerText);
            answer.setScore(BigDecimal.valueOf(graded.score).setScale(3, RoundingMode.HALF_UP));
            answer.setSelectedOptionIds(graded.selectedOptionIds);
            answers.add(answer);

            questionResults.add(new ExerciseResultResponse.QuestionResultDto(
                    q.getId(), graded.score, graded.score >= 1.0,
                    correctOptionIds(q), acceptedAnswers(q)));
        }

        int total = questions.size();
        int scorePercent = total == 0 ? 0 : (int) Math.round((scoreSum / total) * 100);

        HomeworkSubmission saved = submissionRepository.upsertGraded(
                user.getId(), assignmentId, scorePercent, Instant.now());
        answerRepository.saveAll(saved.getId(), answers);

        return new ExerciseResultResponse(scorePercent, fullyCorrect, total, questionResults);
    }

    // --- grading ------------------------------------------------------------

    private record GradedAnswer(double score, String answerText, List<UUID> selectedOptionIds) {}

    private GradedAnswer gradeQuestion(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        return switch (q.getKind()) {
            case SINGLE_CHOICE -> gradeSingleChoice(q, given);
            case MULTI_CHOICE -> gradeMultiChoice(q, given);
            case FILL_BLANK -> gradeFillBlank(q, given);
        };
    }

    private GradedAnswer gradeSingleChoice(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        Set<UUID> correct = q.getOptions().stream()
                .filter(QuestionOption::isCorrect).map(QuestionOption::getId)
                .collect(Collectors.toSet());
        double score = selected.equals(correct) ? 1.0 : 0.0;
        return new GradedAnswer(score, null, new ArrayList<>(selected));
    }

    private GradedAnswer gradeMultiChoice(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        int n = q.getOptions().size();
        if (n == 0) return new GradedAnswer(0.0, null, new ArrayList<>(selected));
        int rightDecisions = 0;
        for (QuestionOption o : q.getOptions()) {
            boolean isSelected = selected.contains(o.getId());
            if (o.isCorrect() && isSelected) rightDecisions++;
            else if (!o.isCorrect() && !isSelected) rightDecisions++;
        }
        double score = (double) rightDecisions / n;
        return new GradedAnswer(score, null, new ArrayList<>(selected));
    }

    private GradedAnswer gradeFillBlank(HomeworkQuestion q, SubmitExerciseRequest.AnswerDto given) {
        String raw = given == null ? null : given.answerText();
        if (raw == null || raw.isBlank()) {
            return new GradedAnswer(0.0, raw, List.of());
        }
        String normalized = normalize(raw);
        boolean matches = q.getOptions().stream()
                .anyMatch(o -> normalize(o.getLabel()).equals(normalized));
        return new GradedAnswer(matches ? 1.0 : 0.0, raw, List.of());
    }

    /** Trim + case-insensitive but accent-exact (no diacritic stripping). */
    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
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
        if (q.getKind() == QuestionKind.FILL_BLANK) return List.of();
        return q.getOptions().stream().filter(QuestionOption::isCorrect).map(QuestionOption::getId).toList();
    }

    private static List<String> acceptedAnswers(HomeworkQuestion q) {
        if (q.getKind() != QuestionKind.FILL_BLANK) return List.of();
        return q.getOptions().stream().map(QuestionOption::getLabel).toList();
    }

    // --- reconstruction for a locked (already graded) exercise --------------

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
            results.add(new ExerciseResultResponse.QuestionResultDto(
                    q.getId(), score, correct, correctOptionIds(q), acceptedAnswers(q)));
        }
        int scorePercent = submission.getScorePercent() == null ? 0 : submission.getScorePercent();
        return new ExerciseResultResponse(scorePercent, fullyCorrect, questions.size(), results);
    }

    private List<ExerciseQuestionDto> buildStudentQuestions(List<HomeworkQuestion> questions) {
        return questions.stream().map(q -> {
            List<ExerciseQuestionDto.StudentOptionDto> options = q.getKind() == QuestionKind.FILL_BLANK
                    ? List.of()
                    : q.getOptions().stream()
                        .map(o -> new ExerciseQuestionDto.StudentOptionDto(o.getId(), o.getLabel()))
                        .toList();
            return new ExerciseQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(), options);
        }).toList();
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
