package com.kuky.backend.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.AudioFileRepository;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.quiz.dto.CreateQuizRequest;
import com.kuky.backend.quiz.dto.QuizQuestionDto;
import com.kuky.backend.quiz.dto.QuizReviewRequest;
import com.kuky.backend.quiz.dto.UpdateQuizRequest;
import com.kuky.backend.quiz.model.Quiz;
import com.kuky.backend.quiz.model.QuizAnswer;
import com.kuky.backend.quiz.model.QuizAttempt;
import com.kuky.backend.quiz.model.QuizAttemptStatus;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.model.QuizSkill;
import com.kuky.backend.quiz.repository.QuizAssigneeRepository;
import com.kuky.backend.quiz.repository.QuizAttemptRepository;
import com.kuky.backend.quiz.repository.QuizQuestionRepository;
import com.kuky.backend.quiz.repository.QuizRepository;
import com.kuky.backend.quiz.service.QuizAdminService;
import com.kuky.backend.quiz.service.QuizGradingService;
import com.kuky.backend.quiz.service.QuizSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizAdminServiceTest {

    private QuizRepository quizRepository;
    private QuizQuestionRepository questionRepository;
    private QuizAssigneeRepository assigneeRepository;
    private QuizAttemptRepository attemptRepository;
    private UserRepository userRepository;
    private QuizAdminService service;
    private QuizSnapshot snapshot;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID quizId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        questionRepository = mock(QuizQuestionRepository.class);
        assigneeRepository = mock(QuizAssigneeRepository.class);
        attemptRepository = mock(QuizAttemptRepository.class);
        userRepository = mock(UserRepository.class);
        snapshot = new QuizSnapshot(objectMapper);
        HomeworkAdminService homeworkAdmin = new HomeworkAdminService(
                mock(ContentRepository.class),
                mock(HomeworkTargetRepository.class),
                mock(HomeworkQuestionRepository.class),
                mock(HomeworkAnswerRepository.class),
                mock(AudioFileRepository.class),
                userRepository,
                mock(HomeworkSubmissionRepository.class),
                mock(ExerciseGradingService.class),
                objectMapper,
                mock(com.kuky.backend.notification.service.NotificationService.class),
                new com.kuky.backend.config.SchedulingProperties());
        service = new QuizAdminService(
                quizRepository, questionRepository, assigneeRepository, attemptRepository,
                userRepository, homeworkAdmin, snapshot,
                new QuizGradingService(mock(ExerciseGradingService.class), objectMapper),
                objectMapper,
                mock(com.kuky.backend.notification.service.NotificationService.class));

        Quiz quiz = quiz();
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(quizRepository.insert(any())).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(quizId);
            return q;
        });
        when(questionRepository.findLiveByQuiz(quizId)).thenReturn(List.of());
        when(assigneeRepository.findAssignees(quizId)).thenReturn(List.of());

        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private Quiz quiz() {
        Quiz q = new Quiz();
        q.setId(quizId);
        q.setTitle("Quiz mixto");
        q.setDescription("desc");
        return q;
    }

    private QuizQuestionDto choice(String skill, String prompt) {
        return new QuizQuestionDto(null, skill, "SINGLE_CHOICE", prompt,
                List.of(new HomeworkQuestionDto.OptionDto(null, "sí", true),
                        new HomeworkQuestionDto.OptionDto(null, "no", false)),
                null, null, null, null);
    }

    @Test
    void assignRejectsQuizWithNoQuestions() {
        when(questionRepository.countLive(quizId)).thenReturn(0);

        assertThatThrownBy(() -> service.setAssignees(quizId, List.of(studentId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pregunta");
        verify(assigneeRepository, never()).replaceAssignees(any(), any());
    }

    @Test
    void assignRejectsNonStudent() {
        when(questionRepository.countLive(quizId)).thenReturn(1);
        User user = new User();
        user.setId(studentId);
        user.setRole("USER");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.setAssignees(quizId, List.of(studentId)))
                .isInstanceOf(StudentNotFoundException.class);
        verify(assigneeRepository, never()).replaceAssignees(any(), any());
    }

    @Test
    void updateSavesMixedSkills() {
        when(questionRepository.findLiveByQuiz(quizId)).thenReturn(List.of());

        service.update(quizId, new UpdateQuizRequest("Quiz mixto", "desc", List.of(
                choice("READING", "¿Hola?"),
                new QuizQuestionDto(null, "WRITING", "FREE_TEXT", "Escribe", List.of(), null, null, null, null),
                choice("GRAMMAR", "¿Ser o estar?"),
                new QuizQuestionDto(null, "LISTENING", "SINGLE_CHOICE", "¿Qué oyes?",
                        List.of(new HomeworkQuestionDto.OptionDto(null, "a", true),
                                new HomeworkQuestionDto.OptionDto(null, "b", false)),
                        null, "AUDIO_URL", "https://example.com/a.mp3", null)
        )));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(questionRepository).replaceQuestions(eq(quizId), captor.capture());
        List<QuizQuestion> saved = captor.getValue();
        assertThat(saved).hasSize(4);
        assertThat(saved.stream().map(QuizQuestion::getSkill).toList())
                .containsExactly(QuizSkill.READING, QuizSkill.WRITING, QuizSkill.GRAMMAR, QuizSkill.LISTENING);
        assertThat(saved.get(3).getAudioUrl()).isEqualTo("https://example.com/a.mp3");
    }

    @Test
    void updateAllowsMultipleQuestionsOnSameSkill() {
        when(questionRepository.findLiveByQuiz(quizId)).thenReturn(List.of());

        service.update(quizId, new UpdateQuizRequest("Gramática", null, List.of(
                choice("GRAMMAR", "¿Ser o estar?"),
                choice("GRAMMAR", "El pretérito")
        )));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(questionRepository).replaceQuestions(eq(quizId), captor.capture());
        List<QuizQuestion> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.stream().map(QuizQuestion::getSkill).toList())
                .containsExactly(QuizSkill.GRAMMAR, QuizSkill.GRAMMAR);
    }

    @Test
    void listeningWithoutMediaIsRejected() {
        assertThatThrownBy(() -> service.update(quizId, new UpdateQuizRequest("Q", null, List.of(
                new QuizQuestionDto(null, "LISTENING", "SINGLE_CHOICE", "¿…?",
                        List.of(new HomeworkQuestionDto.OptionDto(null, "a", true),
                                new HomeworkQuestionDto.OptionDto(null, "b", false)),
                        null, null, null, null)
        )))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audio");
    }

    @Test
    void liveEditDoesNotRewriteExistingSnapshot() {
        QuizQuestion live = new QuizQuestion();
        live.setId(UUID.randomUUID());
        live.setSkill(QuizSkill.READING);
        live.setKind(QuestionKind.SINGLE_CHOICE);
        live.setPrompt("vieja");
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setQuizId(quizId);
        attempt.setQuizSnapshot(snapshot.serialize(quiz(), List.of(live)));
        when(attemptRepository.findByQuiz(quizId)).thenReturn(List.of(attempt));

        service.update(quizId, new UpdateQuizRequest("Quiz mixto", "desc", List.of(
                choice("READING", "nueva pregunta"))));

        verify(attemptRepository, never()).insert(any());
        verify(questionRepository).replaceQuestions(eq(quizId), any());
        assertThat(attempt.getQuizSnapshot()).contains("vieja");
        assertThat(attempt.getQuizSnapshot()).doesNotContain("nueva pregunta");
    }

    @Test
    void unassignDeletesInProgressKeepsSubmitted() {
        when(questionRepository.countLive(quizId)).thenReturn(1);
        UUID other = UUID.randomUUID();
        User otherStudent = new User();
        otherStudent.setId(other);
        otherStudent.setRole("STUDENT");
        when(userRepository.findById(other)).thenReturn(Optional.of(otherStudent));
        when(assigneeRepository.findAssignees(quizId)).thenReturn(List.of(
                new QuizAssigneeRepository.AssigneeView(studentId, "ana@example.com", "Ana", null, null),
                new QuizAssigneeRepository.AssigneeView(other, "bob@example.com", "Bob", null, null)));

        service.setAssignees(quizId, List.of(other));

        verify(attemptRepository).deleteInProgress(quizId, studentId);
        verify(attemptRepository, never()).deleteInProgress(quizId, other);
        verify(assigneeRepository).replaceAssignees(quizId, List.of(other));
    }

    @Test
    void finalizeBlockedUntilAllFreeTextHavePercent() {
        QuizQuestion writing = new QuizQuestion();
        writing.setId(UUID.randomUUID());
        writing.setSkill(QuizSkill.WRITING);
        writing.setKind(QuestionKind.FREE_TEXT);
        writing.setPrompt("Escribe");
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setQuizId(quizId);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setQuizSnapshot(snapshot.serialize(quiz(), List.of(writing)));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        QuizAnswer answer = new QuizAnswer();
        answer.setQuestionId(writing.getId());
        answer.setAnswerText("hola");
        when(attemptRepository.findAnswers(attempt.getId())).thenReturn(List.of(answer));

        assertThatThrownBy(() -> service.review(quizId, attempt.getId(),
                new QuizReviewRequest(List.of(), null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("texto libre");
    }

    @Test
    void reviewRejectedWhenAlreadyGraded() {
        QuizQuestion writing = new QuizQuestion();
        writing.setId(UUID.randomUUID());
        writing.setSkill(QuizSkill.WRITING);
        writing.setKind(QuestionKind.FREE_TEXT);
        writing.setPrompt("Escribe");
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setQuizId(quizId);
        attempt.setStatus(QuizAttemptStatus.GRADED);
        attempt.setQuizSnapshot(snapshot.serialize(quiz(), List.of(writing)));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.review(quizId, attempt.getId(),
                new QuizReviewRequest(List.of(), "nota", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está calificada");
        verify(attemptRepository, never()).replaceAnswers(any(), any());
    }

    @Test
    void reviewUsesSnapshotNotLiveQuiz() {
        QuizQuestion snapQ = new QuizQuestion();
        snapQ.setId(UUID.randomUUID());
        snapQ.setSkill(QuizSkill.READING);
        snapQ.setKind(QuestionKind.SINGLE_CHOICE);
        snapQ.setPrompt("snapshot prompt");
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setQuizId(quizId);
        attempt.setStatus(QuizAttemptStatus.GRADED);
        attempt.setQuizSnapshot(snapshot.serialize(quiz(), List.of(snapQ)));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(attemptRepository.findAnswers(attempt.getId())).thenReturn(List.of());

        QuizQuestion live = new QuizQuestion();
        live.setId(UUID.randomUUID());
        live.setSkill(QuizSkill.GRAMMAR);
        live.setKind(QuestionKind.SINGLE_CHOICE);
        live.setPrompt("live prompt");
        when(questionRepository.findLiveByQuiz(quizId)).thenReturn(List.of(live));

        var view = service.getAttempt(quizId, attempt.getId());
        assertThat(view.questions()).extracting(q -> q.prompt()).containsExactly("snapshot prompt");
        assertThat(view.questions()).extracting(q -> q.skill()).containsExactly("READING");
    }

    @Test
    void reviewQueueMapsSubmittedAttempts() {
        UUID attemptId = UUID.randomUUID();
        java.time.Instant submittedAt = java.time.Instant.parse("2026-08-15T12:00:00Z");
        when(attemptRepository.findSubmittedQueue()).thenReturn(List.of(
                new QuizAttemptRepository.ReviewQueueRow(
                        attemptId, quizId, "Quiz mixto", studentId,
                        "ana@example.com", "Ana", "Lopez", null, submittedAt, false)));

        var queue = service.listReviewQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).attemptId()).isEqualTo(attemptId);
        assertThat(queue.get(0).quizId()).isEqualTo(quizId);
        assertThat(queue.get(0).quizTitle()).isEqualTo("Quiz mixto");
        assertThat(queue.get(0).studentId()).isEqualTo(studentId);
        assertThat(queue.get(0).studentEmail()).isEqualTo("ana@example.com");
        assertThat(queue.get(0).submittedAt()).isEqualTo(submittedAt);
    }

    @Test
    void createReturnsId() {
        var created = service.create(new CreateQuizRequest("Nuevo", null));
        assertThat(created.id()).isEqualTo(quizId);
        assertThat(created.title()).isEqualTo("Nuevo");
    }
}
