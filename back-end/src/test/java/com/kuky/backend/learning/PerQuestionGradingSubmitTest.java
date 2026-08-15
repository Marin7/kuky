package com.kuky.backend.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerQuestionGradingSubmitTest {

    @Mock ContentRepository contentRepository;
    @Mock HomeworkSubmissionRepository submissionRepository;
    @Mock HomeworkQuestionRepository questionRepository;
    @Mock HomeworkAnswerRepository answerRepository;
    @Mock HomeworkTargetRepository targetRepository;
    @Mock UserRepository userRepository;

    private HomeworkSubmissionService service;
    private ExerciseGradingService gradingService;

    private final UUID userId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private static final String EMAIL = "alumno@example.com";

    @BeforeEach
    void setUp() {
        gradingService = new ExerciseGradingService(contentRepository, questionRepository,
                submissionRepository, answerRepository, targetRepository, userRepository, new ObjectMapper());
        service = new HomeworkSubmissionService(contentRepository, submissionRepository, questionRepository,
                answerRepository, targetRepository, userRepository, gradingService,
                new com.kuky.backend.learning.service.AssignmentSnapshot(new ObjectMapper()),
                new SchedulingProperties());

        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        org.mockito.Mockito.lenient().when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        org.mockito.Mockito.lenient().when(targetRepository.isAssignedTo(assignmentId, userId)).thenReturn(true);
    }

    @Test
    void mixedSubmit_setsSubmittedWithAutoScoresAndNullFinalPercent() {
        UUID autoQ = UUID.randomUUID();
        UUID manualQ = UUID.randomUUID();
        HomeworkQuestion auto = choiceQuestion(autoQ, true);
        HomeworkQuestion manual = freeText(manualQ);
        stubAssignment(HomeworkFormat.MIXED, HomeworkType.GRAMMAR, List.of(auto, manual));

        HomeworkSubmission saved = submitted(null);
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), isNull(), any())).thenReturn(saved);

        UUID correct = auto.getOptions().stream().filter(QuestionOption::isCorrect).findFirst().orElseThrow().getId();
        HomeworkItemResponse result = service.submitAnswers(EMAIL, assignmentId, new SubmitExerciseRequest(List.of(
                new SubmitExerciseRequest.AnswerDto(autoQ, List.of(correct), null, null),
                new SubmitExerciseRequest.AnswerDto(manualQ, List.of(), null, "Resumen")),
                Instant.parse("2026-08-15T12:00:00Z")));

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.composition()).isEqualTo("MIXED");
        assertThat(result.scorePercent()).isNull();
        assertThat(result.provisionalScorePercent()).isEqualTo(100);
        assertThat(result.result()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HomeworkAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(eq(saved.getId()), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().stream()
                .filter(a -> autoQ.equals(a.getQuestionId())).findFirst().orElseThrow().getScore())
                .isEqualByComparingTo(BigDecimal.valueOf(1.0).setScale(3));
        assertThat(captor.getValue().stream()
                .filter(a -> manualQ.equals(a.getQuestionId())).findFirst().orElseThrow().getAnswerText())
                .isEqualTo("Resumen");
    }

    @Test
    void allAutoSubmit_gradesImmediately() {
        UUID qid = UUID.randomUUID();
        HomeworkQuestion q = choiceQuestion(qid, true);
        stubAssignment(HomeworkFormat.EXERCISE, HomeworkType.GRAMMAR, List.of(q));
        HomeworkSubmission saved = submitted(100);
        saved.setStatus(HomeworkStatus.GRADED.name());
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsertGraded(eq(userId), eq(assignmentId), eq(100), any())).thenReturn(saved);

        UUID correct = q.getOptions().stream().filter(QuestionOption::isCorrect).findFirst().orElseThrow().getId();
        HomeworkItemResponse result = service.submitAnswers(EMAIL, assignmentId, new SubmitExerciseRequest(List.of(
                new SubmitExerciseRequest.AnswerDto(qid, List.of(correct), null, null)),
                Instant.parse("2026-08-15T12:00:00Z")));

        assertThat(result.status()).isEqualTo("GRADED");
        assertThat(result.composition()).isEqualTo("ALL_AUTO");
        assertThat(result.scorePercent()).isEqualTo(100);
        verify(submissionRepository).upsertGraded(eq(userId), eq(assignmentId), eq(100), any());
    }

    @Test
    void allManualSubmit_viaAnswers_setsSubmittedWithoutScore() {
        UUID qid = UUID.randomUUID();
        HomeworkQuestion q = freeText(qid);
        stubAssignment(HomeworkFormat.MANUAL, HomeworkType.GRAMMAR, List.of(q));
        HomeworkSubmission saved = submitted(null);
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), isNull(), any())).thenReturn(saved);

        HomeworkItemResponse result = service.submitAnswers(EMAIL, assignmentId, new SubmitExerciseRequest(List.of(
                new SubmitExerciseRequest.AnswerDto(qid, List.of(), null, "texto")),
                Instant.parse("2026-08-15T12:00:00Z")));

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.composition()).isEqualTo("ALL_MANUAL");
        assertThat(result.scorePercent()).isNull();
        verify(submissionRepository).upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), isNull(), any());
    }

    @Test
    void incompleteMixedAnswersRejected() {
        UUID autoQ = UUID.randomUUID();
        UUID manualQ = UUID.randomUUID();
        stubAssignment(HomeworkFormat.MIXED, HomeworkType.GRAMMAR,
                List.of(choiceQuestion(autoQ, true), freeText(manualQ)));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitAnswers(EMAIL, assignmentId, new SubmitExerciseRequest(List.of(
                new SubmitExerciseRequest.AnswerDto(autoQ, List.of(), null, null)),
                Instant.parse("2026-08-15T12:00:00Z"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("todas");
    }

    @Test
    void noRetakeAfterSubmitted() {
        UUID qid = UUID.randomUUID();
        stubAssignment(HomeworkFormat.MIXED, HomeworkType.GRAMMAR, List.of(freeText(qid), choiceQuestion(UUID.randomUUID(), false)));
        HomeworkSubmission existing = submitted(null);
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.submitAnswers(EMAIL, assignmentId, new SubmitExerciseRequest(List.of())))
                .isInstanceOf(SubmissionNotAllowedException.class);
    }

    @Test
    void legacyWriteSubmitStillWorks() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(assignmentId);
        a.setTitle("Escritura");
        a.setInstructions("Escribe");
        a.setFormat(HomeworkFormat.MANUAL);
        a.setHomeworkType(HomeworkType.WRITE);
        a.setPublished(true);
        a.setContentRevisedAt(Instant.parse("2026-08-15T12:00:00Z"));
        when(contentRepository.lockAssignment(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        HomeworkSubmission saved = submitted(null);
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), any(), any())).thenReturn(saved);

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId,
                List.of(new com.kuky.backend.learning.model.FormattedTextSegment("hola", null, null, null)),
                null, Instant.parse("2026-08-15T12:00:00Z"));

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.composition()).isEqualTo("WRITE");
    }

    private void stubAssignment(HomeworkFormat format, HomeworkType type, List<HomeworkQuestion> questions) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(assignmentId);
        a.setTitle("Tarea");
        a.setInstructions("Instrucciones");
        a.setFormat(format);
        a.setHomeworkType(type);
        a.setPublished(true);
        a.setContentRevisedAt(Instant.parse("2026-08-15T12:00:00Z"));
        when(contentRepository.lockAssignment(assignmentId)).thenReturn(Optional.of(a));
        org.mockito.Mockito.lenient().when(questionRepository.findByAssignment(assignmentId)).thenReturn(questions);
    }

    private HomeworkSubmission submitted(Integer scorePercent) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setUserId(userId);
        s.setAssignmentId(assignmentId);
        s.setStatus(HomeworkStatus.SUBMITTED.name());
        s.setScorePercent(scorePercent);
        s.setSubmittedAt(Instant.now());
        return s;
    }

    private static HomeworkQuestion freeText(UUID id) {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(id);
        q.setKind(QuestionKind.FREE_TEXT);
        q.setPrompt("Resume");
        q.setOptions(List.of());
        q.setStructureJson("{}");
        return q;
    }

    private static HomeworkQuestion choiceQuestion(UUID id, boolean firstCorrect) {
        QuestionOption a = new QuestionOption();
        a.setId(UUID.randomUUID());
        a.setLabel("A");
        a.setCorrect(firstCorrect);
        QuestionOption b = new QuestionOption();
        b.setId(UUID.randomUUID());
        b.setLabel("B");
        b.setCorrect(!firstCorrect);
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(id);
        q.setKind(QuestionKind.SINGLE_CHOICE);
        q.setPrompt("¿?");
        q.setOptions(List.of(a, b));
        q.setStructureJson("{}");
        return q;
    }
}
