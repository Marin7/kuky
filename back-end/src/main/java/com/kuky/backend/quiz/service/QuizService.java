package com.kuky.backend.quiz.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.quiz.dto.QuizListResponse;
import com.kuky.backend.quiz.dto.QuizTakeResponse;
import com.kuky.backend.quiz.dto.SubmitQuizRequest;
import com.kuky.backend.quiz.exception.QuizAlreadySubmittedException;
import com.kuky.backend.quiz.exception.QuizNotAssignedException;
import com.kuky.backend.quiz.exception.QuizNotFoundException;
import com.kuky.backend.quiz.model.Quiz;
import com.kuky.backend.quiz.model.QuizAnswer;
import com.kuky.backend.quiz.model.QuizAttempt;
import com.kuky.backend.quiz.model.QuizAttemptStatus;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.repository.QuizAssigneeRepository;
import com.kuky.backend.quiz.repository.QuizAttemptRepository;
import com.kuky.backend.quiz.repository.QuizQuestionRepository;
import com.kuky.backend.quiz.repository.QuizRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAssigneeRepository assigneeRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final QuizSnapshot quizSnapshot;
    private final QuizGradingService gradingService;

    public QuizService(QuizRepository quizRepository,
                       QuizQuestionRepository questionRepository,
                       QuizAssigneeRepository assigneeRepository,
                       QuizAttemptRepository attemptRepository,
                       UserRepository userRepository,
                       QuizSnapshot quizSnapshot,
                       QuizGradingService gradingService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.assigneeRepository = assigneeRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.quizSnapshot = quizSnapshot;
        this.gradingService = gradingService;
    }

    public QuizListResponse listMine(String email) {
        User user = requireUser(email);
        Map<UUID, Quiz> byId = new LinkedHashMap<>();
        for (Quiz q : quizRepository.findAssignedToUser(user.getId())) {
            byId.put(q.getId(), q);
        }
        Map<UUID, QuizAttempt> attempts = attemptRepository.findByUser(user.getId()).stream()
                .collect(Collectors.toMap(QuizAttempt::getQuizId, Function.identity(), (a, b) -> a));
        for (QuizAttempt a : attempts.values()) {
            if (a.getStatus() == QuizAttemptStatus.IN_PROGRESS) continue;
            byId.computeIfAbsent(a.getQuizId(), id -> quizRepository.findById(id).orElse(null));
        }

        List<QuizListResponse.QuizListItem> items = new ArrayList<>();
        for (Quiz quiz : byId.values()) {
            if (quiz == null) continue;
            QuizAttempt attempt = attempts.get(quiz.getId());
            String status;
            if (attempt == null) {
                status = "AVAILABLE";
            } else {
                status = attempt.getStatus().name();
            }
            items.add(new QuizListResponse.QuizListItem(
                    quiz.getId(), quiz.getTitle(), quiz.getDescription(), status));
        }
        return new QuizListResponse(items);
    }

    @Transactional
    public QuizTakeResponse getOrStart(String email, UUID quizId) {
        User user = requireUser(email);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz no encontrado."));
        Optional<QuizAttempt> existing = attemptRepository.findByQuizAndUser(quizId, user.getId());
        boolean assigned = assigneeRepository.isAssigned(quizId, user.getId());

        if (existing.isEmpty()) {
            if (!assigned) {
                throw new QuizNotAssignedException("Este quiz no te ha sido asignado.");
            }
            List<QuizQuestion> live = questionRepository.findLiveByQuiz(quizId);
            if (live.isEmpty()) {
                throw new QuizNotFoundException("Quiz no encontrado.");
            }
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuizId(quizId);
            attempt.setUserId(user.getId());
            attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(Instant.now());
            attempt.setQuizSnapshot(quizSnapshot.serialize(quiz, live));
            try {
                attempt = attemptRepository.insert(attempt);
            } catch (DuplicateKeyException e) {
                attempt = attemptRepository.findByQuizAndUser(quizId, user.getId()).orElseThrow();
            }
            return toTake(quiz, attempt, live, List.of(), false);
        }

        QuizAttempt attempt = existing.get();
        List<QuizQuestion> questions = quizSnapshot.questionsOf(attempt.getQuizSnapshot());
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
            return toTake(quiz, attempt, questions, List.of(), false);
        }
        if (!assigned && attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
            throw new QuizNotAssignedException("Este quiz no te ha sido asignado.");
        }
        List<QuizAnswer> answers = attemptRepository.findAnswers(attempt.getId());
        boolean autoOnly = attempt.getStatus() == QuizAttemptStatus.SUBMITTED;
        return toResult(quiz, attempt, questions, answers, autoOnly);
    }

    @Transactional
    public QuizTakeResponse submit(String email, UUID quizId, SubmitQuizRequest request) {
        User user = requireUser(email);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz no encontrado."));
        QuizAttempt attempt = attemptRepository.findByQuizAndUser(quizId, user.getId())
                .orElseThrow(() -> new QuizNotAssignedException("Este quiz no te ha sido asignado."));
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new QuizAlreadySubmittedException("Este quiz ya ha sido entregado.");
        }
        if (!assigneeRepository.isAssigned(quizId, user.getId())) {
            throw new QuizNotAssignedException("Este quiz no te ha sido asignado.");
        }

        List<QuizQuestion> questions = quizSnapshot.questionsOf(attempt.getQuizSnapshot());
        Map<UUID, SubmitExerciseRequest.AnswerDto> byQuestion = (request == null || request.answers() == null)
                ? Map.of()
                : request.answers().stream()
                    .filter(a -> a.questionId() != null)
                    .collect(Collectors.toMap(SubmitExerciseRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));

        QuizGradingService.GradeOutcome outcome = gradingService.gradeSubmit(questions, byQuestion);
        attempt.setSubmittedAt(Instant.now());
        gradingService.applyToAttempt(attempt, outcome);
        attemptRepository.replaceAnswers(attempt.getId(), outcome.answers());
        attemptRepository.updateAfterSubmit(attempt);

        boolean hideTeacher = attempt.getStatus() == QuizAttemptStatus.SUBMITTED;
        QuizGradingService.GradeOutcome view = gradingService.summarize(questions, outcome.answers(), hideTeacher);
        return toResult(quiz, attempt, questions, outcome.answers(), hideTeacher, view);
    }

    private QuizTakeResponse toTake(
            Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions,
            List<QuizAnswer> answers, boolean revealKeys) {
        return new QuizTakeResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                attempt.getStatus().name(),
                gradingService.studentQuestions(questions, revealKeys),
                null, null, null, List.of(), List.of(), null);
    }

    private QuizTakeResponse toResult(
            Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions,
            List<QuizAnswer> answers, boolean autoOnly) {
        QuizGradingService.GradeOutcome outcome = gradingService.summarize(questions, answers, autoOnly);
        return toResult(quiz, attempt, questions, answers, autoOnly, outcome);
    }

    private QuizTakeResponse toResult(
            Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions,
            List<QuizAnswer> answers, boolean autoOnly, QuizGradingService.GradeOutcome outcome) {
        Integer score = attempt.getStatus() == QuizAttemptStatus.GRADED
                ? attempt.getScorePercent()
                : (autoOnly ? outcome.scorePercent() : attempt.getScorePercent());
        Integer fc = attempt.getStatus() == QuizAttemptStatus.GRADED
                ? attempt.getFullyCorrectCount()
                : (autoOnly ? outcome.fullyCorrectCount() : null);
        Integer qc = attempt.getStatus() == QuizAttemptStatus.GRADED
                ? attempt.getQuestionUnitCount()
                : outcome.questionUnitCount();
        var results = outcome.questionResults();
        if (attempt.getStatus() == QuizAttemptStatus.SUBMITTED) {
            results = results.stream().map(gradingService::stripTeacherPercent).toList();
        }
        return new QuizTakeResponse(
                quiz.getId(),
                quizSnapshot.parse(attempt.getQuizSnapshot()).title(),
                quizSnapshot.parse(attempt.getQuizSnapshot()).description(),
                attempt.getStatus().name(),
                gradingService.studentQuestions(questions, true),
                score, fc, qc, outcome.skills(), results,
                attempt.getStatus() == QuizAttemptStatus.GRADED ? attempt.getFeedback() : null);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }
}
