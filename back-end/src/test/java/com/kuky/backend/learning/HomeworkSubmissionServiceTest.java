package com.kuky.backend.learning;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.service.HomeworkSubmissionService;
import com.kuky.backend.learning.model.HomeworkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkSubmissionServiceTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private HomeworkSubmissionRepository submissionRepository;
    @Mock
    private HomeworkQuestionRepository questionRepository;
    @Mock
    private HomeworkAnswerRepository answerRepository;
    @Mock
    private com.kuky.backend.learning.repository.HomeworkTargetRepository targetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.kuky.backend.learning.service.ExerciseGradingService gradingService;

    private HomeworkSubmissionService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private static final String EMAIL = "student@example.com";

    @BeforeEach
    void setUp() {
        service = new HomeworkSubmissionService(contentRepository, submissionRepository, questionRepository,
                answerRepository, targetRepository, userRepository, gradingService, new SchedulingProperties());
        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    private static List<FormattedTextSegment> plain(String text) {
        return List.of(new FormattedTextSegment(text, null, null, null));
    }

    @Test
    void submit_newSubmission_setsSubmittedWithResponse() {
        HomeworkAssignment a = assignment(LocalDate.now().minusDays(1));
        List<FormattedTextSegment> response = plain("Mi respuesta");
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), eq(FormattedTextSegment.toJson(response)), any()))
                .thenReturn(submission(HomeworkStatus.SUBMITTED, FormattedTextSegment.toJson(response)));

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId, response, null);

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.response()).isEqualTo(response);
        // Past-due but now submitted ⇒ not overdue
        assertThat(result.overdue()).isFalse();
    }

    @Test
    void submit_markDoneWithoutText_setsSubmitted() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), isNull(), any()))
                .thenReturn(submission(HomeworkStatus.SUBMITTED, null));

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId, null, null);

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.response()).isNull();
    }

    @Test
    void submit_unknownAssignment_throwsNotFound() {
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId, plain("x"), null))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void submit_whenReviewed_throwsNotAllowed() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId))
                .thenReturn(Optional.of(submission(HomeworkStatus.REVIEWED, FormattedTextSegment.toJson(plain("ya revisada")))));

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId, plain("nuevo intento"), null))
                .isInstanceOf(SubmissionNotAllowedException.class);
    }

    @Test
    void submit_whenAlreadySubmitted_isIdempotentUpdate() {
        HomeworkAssignment a = assignment(null);
        List<FormattedTextSegment> updated = plain("actualizada");
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId))
                .thenReturn(Optional.of(submission(HomeworkStatus.SUBMITTED, FormattedTextSegment.toJson(plain("anterior")))));
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), eq(FormattedTextSegment.toJson(updated)), any()))
                .thenReturn(submission(HomeworkStatus.SUBMITTED, FormattedTextSegment.toJson(updated)));

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId, updated, null);

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.response()).isEqualTo(updated);
    }

    @Test
    void submit_rejectsInvalidColor() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId,
                List.of(new FormattedTextSegment("hola", "purple", null, null)), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submit_rejectsInvalidHighlight() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId,
                List.of(new FormattedTextSegment("hola", null, "orange", null)), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submit_rejectsAnswerOverVisibleLengthLimit_regardlessOfMarkupSize() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        String tooLong = "a".repeat(FormattedTextSegment.MAX_VISIBLE_LENGTH + 1);

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId, plain(tooLong), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submit_multiManual_requiresEveryAnswer() {
        HomeworkAssignment a = assignment(null);
        a.setHomeworkType(null); // non-WRITE MANUAL
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        com.kuky.backend.learning.model.HomeworkQuestion hq1 = new com.kuky.backend.learning.model.HomeworkQuestion();
        hq1.setId(q1);
        hq1.setKind(com.kuky.backend.learning.model.QuestionKind.FREE_TEXT);
        hq1.setPrompt("Q1");
        com.kuky.backend.learning.model.HomeworkQuestion hq2 = new com.kuky.backend.learning.model.HomeworkQuestion();
        hq2.setId(q2);
        hq2.setKind(com.kuky.backend.learning.model.QuestionKind.FREE_TEXT);
        hq2.setPrompt("Q2");
        when(questionRepository.findByAssignment(assignmentId)).thenReturn(List.of(hq1, hq2));

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId, null,
                List.of(new com.kuky.backend.learning.dto.ManualAnswerDto(q1, "solo una"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("todas");
    }

    // ---- helpers ----

    private HomeworkAssignment assignment(LocalDate dueOn) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(assignmentId);
        a.setTitle("Tarea");
        a.setInstructions("Instrucciones");
        a.setDueOn(dueOn);
        a.setPublished(true);
        a.setHomeworkType(HomeworkType.WRITE);
        return a;
    }

    private HomeworkSubmission submission(HomeworkStatus status, String responseJson) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setUserId(userId);
        s.setAssignmentId(assignmentId);
        s.setStatus(status.name());
        s.setResponseText(responseJson);
        s.setSubmittedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }
}
