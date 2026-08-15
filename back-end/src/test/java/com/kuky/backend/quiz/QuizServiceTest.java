package com.kuky.backend.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.quiz.dto.SubmitQuizRequest;
import com.kuky.backend.quiz.exception.QuizAlreadySubmittedException;
import com.kuky.backend.quiz.exception.QuizNotAssignedException;
import com.kuky.backend.quiz.model.Quiz;
import com.kuky.backend.quiz.model.QuizAttempt;
import com.kuky.backend.quiz.model.QuizAttemptStatus;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.model.QuizSkill;
import com.kuky.backend.quiz.repository.QuizAssigneeRepository;
import com.kuky.backend.quiz.repository.QuizAttemptRepository;
import com.kuky.backend.quiz.repository.QuizQuestionRepository;
import com.kuky.backend.quiz.repository.QuizRepository;
import com.kuky.backend.quiz.service.QuizGradingService;
import com.kuky.backend.quiz.service.QuizService;
import com.kuky.backend.quiz.service.QuizSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizServiceTest {

    private QuizRepository quizRepository;
    private QuizQuestionRepository questionRepository;
    private QuizAssigneeRepository assigneeRepository;
    private QuizAttemptRepository attemptRepository;
    private UserRepository userRepository;
    private QuizService service;
    private QuizSnapshot snapshot;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EMAIL = "ana@example.com";
    private final UUID userId = UUID.randomUUID();
    private final UUID quizId = UUID.randomUUID();
    private final UUID correctId = UUID.randomUUID();
    private final UUID wrongId = UUID.randomUUID();
    private final UUID readingId = UUID.randomUUID();
    private final UUID writingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        questionRepository = mock(QuizQuestionRepository.class);
        assigneeRepository = mock(QuizAssigneeRepository.class);
        attemptRepository = mock(QuizAttemptRepository.class);
        userRepository = mock(UserRepository.class);
        snapshot = new QuizSnapshot(objectMapper);
        ExerciseGradingService exerciseGrading = mock(ExerciseGradingService.class);
        when(exerciseGrading.studentQuestionsFor(any())).thenReturn(List.of());
        service = new QuizService(quizRepository, questionRepository, assigneeRepository, attemptRepository,
                userRepository, snapshot, new QuizGradingService(exerciseGrading, objectMapper));

        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        user.setRole("STUDENT");
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setTitle("Quiz");
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(assigneeRepository.isAssigned(quizId, userId)).thenReturn(true);
    }

    private QuizQuestion reading() {
        QuizQuestion q = new QuizQuestion();
        q.setId(readingId);
        q.setSkill(QuizSkill.READING);
        q.setKind(QuestionKind.SINGLE_CHOICE);
        q.setPrompt("¿Hola?");
        QuestionOption yes = new QuestionOption();
        yes.setId(correctId);
        yes.setLabel("sí");
        yes.setCorrect(true);
        QuestionOption no = new QuestionOption();
        no.setId(wrongId);
        no.setLabel("no");
        no.setCorrect(false);
        q.setOptions(List.of(yes, no));
        return q;
    }

    private QuizQuestion writing() {
        QuizQuestion q = new QuizQuestion();
        q.setId(writingId);
        q.setSkill(QuizSkill.WRITING);
        q.setKind(QuestionKind.FREE_TEXT);
        q.setPrompt("Escribe");
        return q;
    }

    private QuizAttempt inProgress(List<QuizQuestion> questions) {
        QuizAttempt a = new QuizAttempt();
        a.setId(UUID.randomUUID());
        a.setQuizId(quizId);
        a.setUserId(userId);
        a.setStatus(QuizAttemptStatus.IN_PROGRESS);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        a.setQuizSnapshot(snapshot.serialize(quiz, questions));
        return a;
    }

    @Test
    void listIncludesOnlyAssignedQuizzes() {
        Quiz assigned = new Quiz();
        assigned.setId(quizId);
        assigned.setTitle("Mío");
        when(quizRepository.findAssignedToUser(userId)).thenReturn(List.of(assigned));
        when(attemptRepository.findByUser(userId)).thenReturn(List.of());

        var list = service.listMine(EMAIL);
        assertThat(list.quizzes()).hasSize(1);
        assertThat(list.quizzes().get(0).status()).isEqualTo("AVAILABLE");
    }

    @Test
    void unassignedWithoutAttemptIsForbidden() {
        when(assigneeRepository.isAssigned(quizId, userId)).thenReturn(false);
        when(attemptRepository.findByQuizAndUser(quizId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrStart(EMAIL, quizId))
                .isInstanceOf(QuizNotAssignedException.class);
        verify(attemptRepository, never()).insert(any());
    }

    @Test
    void snapshotAtStartIgnoresLaterLiveEdits() {
        QuizQuestion original = reading();
        when(questionRepository.findLiveByQuiz(quizId)).thenReturn(List.of(original));
        when(attemptRepository.findByQuizAndUser(quizId, userId)).thenReturn(Optional.empty());
        when(attemptRepository.insert(any())).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        service.getOrStart(EMAIL, quizId);

        QuizQuestion edited = reading();
        edited.setPrompt("edited live");
        when(questionRepository.findLiveByQuiz(quizId)).thenReturn(List.of(edited));

        QuizAttempt stored = inProgress(List.of(original));
        when(attemptRepository.findByQuizAndUser(quizId, userId)).thenReturn(Optional.of(stored));

        var take = service.getOrStart(EMAIL, quizId);
        assertThat(stored.getQuizSnapshot()).contains("¿Hola?");
        assertThat(stored.getQuizSnapshot()).doesNotContain("edited live");
        assertThat(take.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void allAutoSubmitIsGradedWithSkillBreakdown() {
        QuizAttempt attempt = inProgress(List.of(reading()));
        when(attemptRepository.findByQuizAndUser(quizId, userId)).thenReturn(Optional.of(attempt));

        var result = service.submit(EMAIL, quizId, new SubmitQuizRequest(List.of(
                new SubmitExerciseRequest.AnswerDto(readingId, List.of(correctId), null))));

        assertThat(result.status()).isEqualTo("GRADED");
        assertThat(result.scorePercent()).isEqualTo(100);
        assertThat(result.skills()).hasSize(1);
        assertThat(result.skills().get(0).skill()).isEqualTo("READING");
        assertThat(result.skills().get(0).scorePercent()).isEqualTo(100);
        assertThat(result.skills().get(0).awaitingTeacher()).isFalse();
        assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.GRADED);
    }

    @Test
    void freeTextStaysSubmitted() {
        QuizAttempt attempt = inProgress(List.of(reading(), writing()));
        when(attemptRepository.findByQuizAndUser(quizId, userId)).thenReturn(Optional.of(attempt));

        var result = service.submit(EMAIL, quizId, new SubmitQuizRequest(List.of(
                new SubmitExerciseRequest.AnswerDto(readingId, List.of(correctId), null),
                new SubmitExerciseRequest.AnswerDto(writingId, List.of(), null, "mi texto"))));

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.skills()).anyMatch(s -> "WRITING".equals(s.skill()) && s.awaitingTeacher());
        assertThat(result.skills()).anyMatch(s -> "READING".equals(s.skill()) && !s.awaitingTeacher());
        assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.SUBMITTED);
    }

    @Test
    void secondSubmitIsConflict() {
        QuizAttempt attempt = inProgress(List.of(reading()));
        attempt.setStatus(QuizAttemptStatus.GRADED);
        when(attemptRepository.findByQuizAndUser(quizId, userId)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.submit(EMAIL, quizId, new SubmitQuizRequest(List.of())))
                .isInstanceOf(QuizAlreadySubmittedException.class);
    }
}
