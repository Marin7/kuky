package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.AlreadyReviewedException;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.NotSubmittedException;
import com.kuky.backend.learning.exception.SubmissionNotFoundException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.AudioFileRepository;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.ExerciseGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HomeworkAdminServiceTest {

    private ContentRepository contentRepository;
    private HomeworkTargetRepository targetRepository;
    private HomeworkQuestionRepository questionRepository;
    private AudioFileRepository audioFileRepository;
    private UserRepository userRepository;
    private HomeworkSubmissionRepository submissionRepository;
    private HomeworkAdminService service;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        targetRepository = mock(HomeworkTargetRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        audioFileRepository = mock(AudioFileRepository.class);
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(HomeworkSubmissionRepository.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                audioFileRepository, userRepository, submissionRepository, mock(ExerciseGradingService.class),
                new ObjectMapper());

        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private HomeworkAssignment assignment(UUID id) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(id);
        a.setTitle("Tarea");
        a.setInstructions("Hazla");
        a.setDueOn(LocalDate.of(2026, 6, 20));
        return a;
    }

    @Test
    void createAssignsTargetsAndReturnsItem() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of(
                new HomeworkTargetRepository.AssigneeView(studentId, "ana@example.com",
                        null, null, null, "SUBMITTED", "Mi respuesta", Instant.now(), null, null, false)));

        HomeworkAdminItem item = service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", LocalDate.of(2026, 6, 20), null, null, "MANUAL", List.of(), null, null, List.of(studentId)));

        verify(targetRepository).replaceTargets(id, List.of(studentId));
        assertThat(item.assignees()).hasSize(1);
        assertThat(item.assignees().get(0).status()).isEqualTo("SUBMITTED");
        assertThat(item.assignees().get(0).responseText()).isEqualTo("Mi respuesta");
    }

    @Test
    void createRejectsUnknownStudent() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, null, null, "MANUAL", List.of(), null, null, List.of(unknown))))
                .isInstanceOf(StudentNotFoundException.class);
        verify(contentRepository, never()).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void setAssigneesReplacesTargets() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.setAssignees(id, List.of(studentId));

        verify(targetRepository).replaceTargets(id, List.of(studentId));
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(contentRepository.deleteAssignment(id)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void updateThrowsWhenAssignmentMissing() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id,
                new com.kuky.backend.admin.dto.UpdateHomeworkRequest("T", "I", null, null, null, "MANUAL", List.of(), null, null)))
                .isInstanceOf(AssignmentNotFoundException.class);
        verify(contentRepository, never()).updateAssignment(eq(id), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // --- Teacher review of MANUAL submissions --------------------------------
    // Note: the review queue's actual MANUAL/SUBMITTED filtering happens in the
    // repository's SQL (verified by HomeworkAdminControllerIntegrationTest against
    // a real database) — this unit test only verifies the service maps whatever
    // the (mocked) repository returns into the response DTO.

    @Test
    void reviewQueueMapsRepositoryRowsToDto() {
        UUID submissionId = UUID.randomUUID();
        Instant submittedAt = Instant.now();
        when(submissionRepository.findSubmittedManualQueue()).thenReturn(List.of(
                new HomeworkSubmissionRepository.ReviewQueueRow(submissionId, studentId, "ana@example.com",
                        "Ana", "Lopez", null, "Tarea", submittedAt)));

        List<HomeworkReviewQueueItemDto> queue = service.getReviewQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).submissionId()).isEqualTo(submissionId);
        assertThat(queue.get(0).studentId()).isEqualTo(studentId);
        assertThat(queue.get(0).assignmentTitle()).isEqualTo("Tarea");
        assertThat(queue.get(0).submittedAt()).isEqualTo(submittedAt);
    }

    @Test
    void getSubmissionDetail_unknownId_throwsNotFound() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubmissionDetail(submissionId))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    @Test
    void saveFeedback_rejectsEmptySegments() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(submissionRepository, never()).updateFeedback(any(), any(), any());
    }

    @Test
    void saveFeedback_rejectsFeedbackOverVisibleLengthLimit() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null)));
        String tooLong = "a".repeat(FormattedTextSegment.MAX_VISIBLE_LENGTH + 1);

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                List.of(new FormattedTextSegment(tooLong, null, null, null))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(submissionRepository, never()).updateFeedback(any(), any(), any());
    }

    @Test
    void saveFeedback_acceptsColorHighlightAndStrikeCombinedOnOneSegment() {
        UUID submissionId = UUID.randomUUID();
        List<FormattedTextSegment> combined =
                List.of(new FormattedTextSegment("bien", "red", "yellow", true));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null)))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", FormattedTextSegment.toJson(combined))));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId, combined);

        assertThat(result.feedback()).hasSize(1);
        assertThat(result.feedback().get(0).color()).isEqualTo("red");
        assertThat(result.feedback().get(0).highlight()).isEqualTo("yellow");
        assertThat(result.feedback().get(0).strike()).isTrue();
    }

    @Test
    void saveFeedback_handCraftedMarkupIsStoredAsInertPlainText() {
        // Guarantees SC-004 server-side: markup-like text is never interpreted,
        // only ever carried as the segment's plain `text` value.
        UUID submissionId = UUID.randomUUID();
        String malicious = "<script>alert(1)</script>";
        List<FormattedTextSegment> feedback = List.of(new FormattedTextSegment(malicious, null, null, null));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null)))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", FormattedTextSegment.toJson(feedback))));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId, feedback);

        assertThat(result.feedback().get(0).text()).isEqualTo(malicious);
    }

    @Test
    void saveFeedback_alreadyReviewed_throws() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                List.of(new FormattedTextSegment("ok", null, null, null))))
                .isInstanceOf(AlreadyReviewedException.class);
        verify(submissionRepository, never()).updateFeedback(any(), any(), any());
    }

    @Test
    void saveFeedback_notYetSubmitted_throws() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "PENDING", null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                List.of(new FormattedTextSegment("ok", null, null, null))))
                .isInstanceOf(NotSubmittedException.class);
        verify(submissionRepository, never()).updateFeedback(any(), any(), any());
    }

    @Test
    void saveFeedback_success_transitionsToReviewed() {
        UUID submissionId = UUID.randomUUID();
        List<FormattedTextSegment> feedback = List.of(new FormattedTextSegment("Muy bien", null, null, null));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null)))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", FormattedTextSegment.toJson(feedback))));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId, feedback);

        assertThat(result.status()).isEqualTo("REVIEWED");
        verify(submissionRepository).updateFeedback(eq(submissionId), any(), any());
    }

    // --- Plain-text feedback on GRADED exercises -----------------------------

    @Test
    void saveExerciseFeedback_savesOnGradedExercise() {
        UUID submissionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        HomeworkSubmission graded = gradedSubmission(submissionId, assignmentId, null);
        HomeworkSubmission afterSave = gradedSubmission(submissionId, assignmentId, "Muy bien");
        HomeworkAssignment exercise = exerciseAssignment(assignmentId);
        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(graded))
                .thenReturn(Optional.of(afterSave));
        when(contentRepository.findAssignmentById(assignmentId)).thenReturn(Optional.of(exercise));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser()));
        ExerciseGradingService grading = mock(ExerciseGradingService.class);
        when(grading.viewGradedSubmission(any())).thenReturn(
                new ExerciseGradingService.GradedExerciseView(List.of(),
                        new com.kuky.backend.learning.dto.ExerciseResultResponse(100, 1, 1, List.of())));
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                audioFileRepository, userRepository, submissionRepository, grading, new ObjectMapper());

        var result = service.saveExerciseFeedback(submissionId, "  Muy bien  ");

        verify(submissionRepository).updateExerciseFeedback(eq(submissionId),
                eq(FormattedTextSegment.encodePlainFeedback("Muy bien")));
        assertThat(result.teacherFeedback()).isEqualTo("Muy bien");
    }

    @Test
    void saveExerciseFeedback_clearSetsNull() {
        UUID submissionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        HomeworkSubmission graded = gradedSubmission(submissionId, assignmentId, "Old");
        HomeworkSubmission cleared = gradedSubmission(submissionId, assignmentId, null);
        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(graded))
                .thenReturn(Optional.of(cleared));
        when(contentRepository.findAssignmentById(assignmentId))
                .thenReturn(Optional.of(exerciseAssignment(assignmentId)));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser()));
        ExerciseGradingService grading = mock(ExerciseGradingService.class);
        when(grading.viewGradedSubmission(any())).thenReturn(
                new ExerciseGradingService.GradedExerciseView(List.of(),
                        new com.kuky.backend.learning.dto.ExerciseResultResponse(80, 0, 1, List.of())));
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                audioFileRepository, userRepository, submissionRepository, grading, new ObjectMapper());

        var result = service.saveExerciseFeedback(submissionId, "   ");

        verify(submissionRepository).updateExerciseFeedback(submissionId, null);
        assertThat(result.teacherFeedback()).isNull();
    }

    @Test
    void saveExerciseFeedback_rejectsOverLengthWithoutUpdating() {
        UUID submissionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(gradedSubmission(submissionId, assignmentId, null)));
        when(contentRepository.findAssignmentById(assignmentId))
                .thenReturn(Optional.of(exerciseAssignment(assignmentId)));

        assertThatThrownBy(() -> service.saveExerciseFeedback(submissionId,
                "a".repeat(FormattedTextSegment.MAX_VISIBLE_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(submissionRepository, never()).updateExerciseFeedback(any(), any());
    }

    @Test
    void saveExerciseFeedback_rejectsNonGraded() {
        UUID submissionId = UUID.randomUUID();
        HomeworkSubmission pending = gradedSubmission(submissionId, UUID.randomUUID(), null);
        pending.setStatus("PENDING");
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.saveExerciseFeedback(submissionId, "ok"))
                .isInstanceOf(NotSubmittedException.class);
        verify(submissionRepository, never()).updateExerciseFeedback(any(), any());
    }

    @Test
    void saveExerciseFeedback_rejectsManualAssignment() {
        UUID submissionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(gradedSubmission(submissionId, assignmentId, null)));
        HomeworkAssignment manual = exerciseAssignment(assignmentId);
        manual.setFormat(com.kuky.backend.learning.model.HomeworkFormat.MANUAL);
        when(contentRepository.findAssignmentById(assignmentId)).thenReturn(Optional.of(manual));

        assertThatThrownBy(() -> service.saveExerciseFeedback(submissionId, "ok"))
                .isInstanceOf(AssignmentNotFoundException.class);
        verify(submissionRepository, never()).updateExerciseFeedback(any(), any());
    }

    private User studentUser() {
        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        return student;
    }

    private HomeworkSubmission gradedSubmission(UUID submissionId, UUID assignmentId, String feedbackPlain) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(submissionId);
        s.setAssignmentId(assignmentId);
        s.setUserId(studentId);
        s.setStatus("GRADED");
        s.setScorePercent(100);
        s.setFeedback(feedbackPlain == null ? null : FormattedTextSegment.encodePlainFeedback(feedbackPlain));
        return s;
    }

    private HomeworkAssignment exerciseAssignment(UUID id) {
        HomeworkAssignment a = assignment(id);
        a.setFormat(com.kuky.backend.learning.model.HomeworkFormat.EXERCISE);
        return a;
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRow(UUID submissionId, String status,
                                                                       String feedbackJson) {
        String response = FormattedTextSegment.toJson(List.of(new FormattedTextSegment("Mi respuesta", null, null, null)));
        return new HomeworkSubmissionRepository.SubmissionDetailRow(
                submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                "Tarea", status, response, feedbackJson, Instant.now(),
                "REVIEWED".equals(status) ? Instant.now() : null);
    }
}
