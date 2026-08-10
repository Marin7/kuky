package com.kuky.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.AudioFileRepository;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.ExerciseGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MixedHomeworkFinalizeTest {

    private ContentRepository contentRepository;
    private HomeworkQuestionRepository questionRepository;
    private HomeworkAnswerRepository answerRepository;
    private HomeworkSubmissionRepository submissionRepository;
    private HomeworkAdminService service;
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        answerRepository = mock(HomeworkAnswerRepository.class);
        submissionRepository = mock(HomeworkSubmissionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        service = new HomeworkAdminService(contentRepository, mock(HomeworkTargetRepository.class),
                questionRepository, answerRepository, mock(AudioFileRepository.class),
                userRepository, submissionRepository, mock(ExerciseGradingService.class), new ObjectMapper());
        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    @Test
    void missingTeacherValidationRejected() {
        UUID submissionId = UUID.randomUUID();
        UUID autoQ = UUID.randomUUID();
        UUID manualQ = UUID.randomUUID();
        stubMixed(submissionId, autoQ, manualQ, BigDecimal.ONE);

        assertThatThrownBy(() -> service.saveFeedback(submissionId, new SaveHomeworkFeedbackRequest(
                "nota", null, List.of(new SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest(
                        manualQ, List.of(new FormattedTextSegment("texto", null, null, null)), null)), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validar");
        verify(submissionRepository, never()).saveScoredAnnotatedReview(any(), any(), any(), anyInt(), anyBoolean());
        verify(submissionRepository, never()).saveMixedGradedReview(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void finalizeComputesCombinedScoreAndGrades() {
        UUID submissionId = UUID.randomUUID();
        UUID autoQ = UUID.randomUUID();
        UUID manualQ = UUID.randomUUID();
        // auto scored 1.0, invalidate manual → mean 0.5 → 50%
        stubMixed(submissionId, autoQ, manualQ, BigDecimal.ONE);

        HomeworkAnswer refreshedManual = freeTextAnswer(manualQ, "texto");
        refreshedManual.setScore(BigDecimal.ZERO.setScale(3));
        refreshedManual.setTeacherValidation("INVALIDATED");
        HomeworkAnswer autoAnswer = autoAnswer(autoQ, BigDecimal.ONE);
        when(answerRepository.findBySubmission(submissionId))
                .thenReturn(List.of(autoAnswer, freeTextAnswer(manualQ, "texto")))
                .thenReturn(List.of(autoAnswer, refreshedManual))
                .thenReturn(List.of(autoAnswer, refreshedManual));

        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detail(submissionId, "SUBMITTED")))
                .thenReturn(Optional.of(detail(submissionId, "GRADED")));

        service.saveFeedback(submissionId, new SaveHomeworkFeedbackRequest(
                "Bien", null, List.of(new SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest(
                        manualQ,
                        List.of(new FormattedTextSegment("texto", null, null, true)),
                        "INVALIDATED")), null));

        verify(answerRepository).updateManualReview(any(), any(), eq("INVALIDATED"), any());
        verify(submissionRepository).saveScoredAnnotatedReview(eq(submissionId), any(), any(), eq(50), eq(true));
    }

    @Test
    void reEditAfterGradedRecalculates() {
        UUID submissionId = UUID.randomUUID();
        UUID autoQ = UUID.randomUUID();
        UUID manualQ = UUID.randomUUID();
        stubMixed(submissionId, autoQ, manualQ, BigDecimal.ONE);

        HomeworkAnswer refreshedManual = freeTextAnswer(manualQ, "texto");
        refreshedManual.setScore(BigDecimal.ONE.setScale(3));
        refreshedManual.setTeacherValidation("VALIDATED");
        HomeworkAnswer autoAnswer = autoAnswer(autoQ, BigDecimal.ONE);
        when(answerRepository.findBySubmission(submissionId))
                .thenReturn(List.of(autoAnswer, freeTextAnswer(manualQ, "texto")))
                .thenReturn(List.of(autoAnswer, refreshedManual))
                .thenReturn(List.of(autoAnswer, refreshedManual));

        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detail(submissionId, "GRADED")))
                .thenReturn(Optional.of(detail(submissionId, "GRADED")));
        // Force ANNOTATED review model for re-edit
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(new HomeworkSubmissionRepository.SubmissionDetailRow(
                        submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                        "Tarea", "GRADED", null, null, "ANNOTATED", 50, "MIXED", "AUDIO", Instant.now(), Instant.now())))
                .thenReturn(Optional.of(new HomeworkSubmissionRepository.SubmissionDetailRow(
                        submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                        "Tarea", "GRADED", null, null, "ANNOTATED", 100, "MIXED", "AUDIO", Instant.now(), Instant.now())));

        service.saveFeedback(submissionId, new SaveHomeworkFeedbackRequest(
                null, null, List.of(new SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest(
                        manualQ,
                        List.of(new FormattedTextSegment("texto", null, null, null)),
                        "VALIDATED")), null));

        verify(submissionRepository).saveScoredAnnotatedReview(eq(submissionId), any(), any(), eq(100), eq(false));
    }

    private void stubMixed(UUID submissionId, UUID autoQ, UUID manualQ, BigDecimal autoScore) {
        UUID assignmentId = UUID.randomUUID();
        HomeworkSubmission sub = new HomeworkSubmission();
        sub.setId(submissionId);
        sub.setAssignmentId(assignmentId);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sub));

        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(assignmentId);
        a.setFormat(HomeworkFormat.MIXED);
        a.setHomeworkType(HomeworkType.AUDIO);
        when(contentRepository.findAssignmentById(assignmentId)).thenReturn(Optional.of(a));

        HomeworkQuestion auto = new HomeworkQuestion();
        auto.setId(autoQ);
        auto.setKind(QuestionKind.SINGLE_CHOICE);
        auto.setPrompt("auto");
        HomeworkQuestion manual = new HomeworkQuestion();
        manual.setId(manualQ);
        manual.setKind(QuestionKind.FREE_TEXT);
        manual.setPrompt("manual");
        when(questionRepository.findByAssignment(assignmentId)).thenReturn(List.of(auto, manual));

        when(answerRepository.findBySubmission(submissionId))
                .thenReturn(List.of(autoAnswer(autoQ, autoScore), freeTextAnswer(manualQ, "texto")));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detail(submissionId, "SUBMITTED")));
    }

    private static HomeworkAnswer autoAnswer(UUID questionId, BigDecimal score) {
        HomeworkAnswer a = new HomeworkAnswer();
        a.setId(UUID.randomUUID());
        a.setQuestionId(questionId);
        a.setScore(score);
        return a;
    }

    private static HomeworkAnswer freeTextAnswer(UUID questionId, String text) {
        HomeworkAnswer a = new HomeworkAnswer();
        a.setId(UUID.randomUUID());
        a.setQuestionId(questionId);
        a.setAnswerText(text);
        a.setPromptSnapshot("manual");
        a.setScore(BigDecimal.ZERO);
        return a;
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detail(UUID submissionId, String status) {
        return new HomeworkSubmissionRepository.SubmissionDetailRow(
                submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                "Tarea", status, null, null,
                "GRADED".equals(status) ? "ANNOTATED" : null,
                "GRADED".equals(status) ? 50 : null,
                "MIXED", "AUDIO",
                Instant.now(),
                "GRADED".equals(status) ? Instant.now() : null);
    }
}
