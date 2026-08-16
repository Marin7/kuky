package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
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
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.ExerciseGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private HomeworkAnswerRepository answerRepository;
    private com.kuky.backend.notification.service.NotificationService notificationService;
    private HomeworkAdminService service;

    private final UUID studentId = UUID.randomUUID();

    private static LocalDate teacherToday() {
        return LocalDate.now(ZoneId.of("Europe/Bucharest"));
    }

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        targetRepository = mock(HomeworkTargetRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        audioFileRepository = mock(AudioFileRepository.class);
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(HomeworkSubmissionRepository.class);
        answerRepository = mock(HomeworkAnswerRepository.class);
        notificationService = mock(com.kuky.backend.notification.service.NotificationService.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                answerRepository,
                audioFileRepository, userRepository, submissionRepository, mock(ExerciseGradingService.class),
                new ObjectMapper(), notificationService,
                new com.kuky.backend.config.SchedulingProperties());

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
        return a;
    }

    @Test
    void createAssignsTargetsAndReturnsItem() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of(
                new HomeworkTargetRepository.AssigneeView(studentId, "ana@example.com",
                        null, null, null, "SUBMITTED", "Mi respuesta", Instant.now(), null, null, false, false, null)));

        HomeworkAdminItem item = service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", teacherToday().plusDays(5), "WRITE", null, "MANUAL", List.of(), null, null, null, null, List.of(studentId)));

        verify(targetRepository).replaceTargets(id, List.of(studentId), teacherToday().plusDays(5));
        assertThat(item.assignees()).hasSize(1);
        assertThat(item.assignees().get(0).status()).isEqualTo("SUBMITTED");
        assertThat(item.assignees().get(0).responseText()).isEqualTo("Mi respuesta");
    }

    @Test
    void createRejectsUnknownStudent() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, null, null, List.of(unknown))))
                .isInstanceOf(StudentNotFoundException.class);
        verify(contentRepository, never()).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void setAssigneesReplacesTargets() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.setAssignees(id, List.of(studentId), null);

        verify(targetRepository).replaceTargets(id, List.of(studentId), null);
    }

    @Test
    void setAssigneesPassesDueOnForNewTargets() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.setAssignees(id, List.of(studentId), teacherToday().plusDays(10));

        verify(targetRepository).replaceTargets(id, List.of(studentId), teacherToday().plusDays(10));
    }

    @Test
    void setAssigneesAppliesPerStudentDueOns() {
        UUID id = UUID.randomUUID();
        LocalDate due = teacherToday().plusDays(4);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());
        when(targetRepository.updateDueOn(id, studentId, due)).thenReturn(1);

        service.setAssignees(id, List.of(studentId), null,
                List.of(new com.kuky.backend.admin.dto.SetAssigneesRequest.DueOn(studentId, due)));

        verify(targetRepository).replaceTargets(id, List.of(studentId), null);
        verify(targetRepository).updateDueOn(id, studentId, due);
    }

    @Test
    void setAssigneesEmptyListStillCallsReplace() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.setAssignees(id, List.of(), teacherToday().plusDays(10));

        verify(targetRepository).replaceTargets(id, List.of(), teacherToday().plusDays(10));
    }

    @Test
    void updateAssigneeDueOnUpdatesThatRow() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.updateDueOn(id, studentId, teacherToday().plusDays(14))).thenReturn(1);
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.updateAssigneeDueOn(id, studentId, teacherToday().plusDays(14));

        verify(targetRepository).updateDueOn(id, studentId, teacherToday().plusDays(14));
        verify(contentRepository, never()).updateAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateAssigneeDueOnRejectsPastDate() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));

        assertThatThrownBy(() -> service.updateAssigneeDueOn(id, studentId, teacherToday().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anterior");
        verify(targetRepository, never()).updateDueOn(any(), any(), any());
    }

    @Test
    void updateAssigneeDueOnAllowsToday() {
        UUID id = UUID.randomUUID();
        LocalDate today = teacherToday();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.updateDueOn(id, studentId, today)).thenReturn(1);
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.updateAssigneeDueOn(id, studentId, today);

        verify(targetRepository).updateDueOn(id, studentId, today);
    }

    @Test
    void setAssigneesRejectsPastDueOn() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));

        assertThatThrownBy(() -> service.setAssignees(id, List.of(studentId), teacherToday().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(targetRepository, never()).replaceTargets(any(), any(), any());
    }

    @Test
    void createRejectsPastDueOn() {
        assertThatThrownBy(() -> service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", teacherToday().minusDays(1), "WRITE", null, "MANUAL",
                List.of(), null, null, null, null, List.of(studentId))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(contentRepository, never()).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateAssigneeDueOnThrowsWhenNotAssigned() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.updateDueOn(id, studentId, null)).thenReturn(0);

        assertThatThrownBy(() -> service.updateAssigneeDueOn(id, studentId, null))
                .isInstanceOf(StudentNotFoundException.class);
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
                new com.kuky.backend.admin.dto.UpdateHomeworkRequest("T", "I", "WRITE", null, "MANUAL", List.of(), null, null, null, null)))
                .isInstanceOf(AssignmentNotFoundException.class);
        verify(contentRepository, never()).updateAssignment(eq(id), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createEmptyAssigneesIgnoresDueOn() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", teacherToday().plusDays(5), "WRITE", null, "MANUAL",
                List.of(), null, null, null, null, List.of()));

        verify(targetRepository, never()).replaceTargets(any(), any(), any());
    }

    @Test
    void createTrimsLabelAndPersists() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, null, List.of("  Subjuntivo  "), List.of()));

        verify(contentRepository).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), eq(List.of("Subjuntivo")));
    }

    @Test
    void createBlankLabelsStoresEmpty() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, null, List.of("   "), List.of()));

        verify(contentRepository).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), eq(List.of()));
    }

    @Test
    void createRejectsLabelLongerThan40() {
        String tooLong = "a".repeat(41);
        assertThatThrownBy(() -> service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, null, List.of(tooLong), List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("40");
        verify(contentRepository, never()).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createAcceptsLabelOf40Characters() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());
        String forty = "á".repeat(40);

        service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, null, List.of(forty), List.of()));

        verify(contentRepository).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), eq(List.of(forty)));
    }

    @Test
    void createKeepsMultipleLabelsAndDedupesCase() {
        UUID id = UUID.randomUUID();
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(assignment(id)));
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.create(new CreateHomeworkRequest(
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, null,
                List.of("  Subjuntivo  ", "B1", "subjuntivo", "  "), List.of()));

        verify(contentRepository).insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(),
                eq(List.of("Subjuntivo", "B1")));
    }

    @Test
    void updateLabelOnlyDoesNotBumpContentRevisedAt() {
        UUID id = UUID.randomUUID();
        HomeworkAssignment existing = assignment(id);
        existing.setHomeworkType(com.kuky.backend.learning.model.HomeworkType.WRITE);
        existing.setInstructions("Hazla");
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(existing));
        when(questionRepository.findByAssignment(id)).thenReturn(List.of());
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        service.update(id, new UpdateHomeworkRequest(
                "Tarea", "Hazla", "WRITE", null, "MANUAL",
                List.of(), null, null, null, List.of("Tema", "Gramática")));

        verify(contentRepository).updateAssignment(eq(id), eq("Tarea"), eq("Hazla"),
                any(), any(), any(), any(), any(), any(), eq(List.of("Tema", "Gramática")), isNull());
    }

    @Test
    void updateLabelsPersistsWithoutFullUpdate() {
        UUID id = UUID.randomUUID();
        HomeworkAssignment existing = assignment(id);
        when(contentRepository.findAssignmentById(id)).thenReturn(Optional.of(existing));
        when(contentRepository.updateLabels(id, List.of("Subjuntivo", "B1"))).thenReturn(1);
        when(questionRepository.findByAssignment(id)).thenReturn(List.of());
        when(targetRepository.findAssigneesWithSubmissions(id)).thenReturn(List.of());

        HomeworkAdminItem item = service.updateLabels(id, List.of("  Subjuntivo  ", "B1"));

        verify(contentRepository).updateLabels(id, List.of("Subjuntivo", "B1"));
        verify(contentRepository, never()).updateAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(item.id()).isEqualTo(id);
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
                        "Ana", "Lopez", null, "Tarea", submittedAt, false)));

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
    void saveFeedback_rejectsChangedStudentWording() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Texto cambiado", null, 100, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("respuesta del alumno");
        verify(submissionRepository, never()).saveAnnotatedReview(any(), any(), any(), anyBoolean());
        verify(submissionRepository, never()).saveScoredAnnotatedReview(any(), any(), any(), anyInt(), any(), anyBoolean());
    }

    @Test
    void saveFeedback_finalizesWriteWithPercent() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Mi respuesta", "red", "yellow", true));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED",
                        FormattedTextSegment.encodePlainFeedback("Bien", 500), "ANNOTATED", 70, 70)));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("Bien", annotated, null, 70, true));

        assertThat(result.reviewModel()).isEqualTo("ANNOTATED");
        assertThat(result.feedbackText()).isEqualTo("Bien");
        assertThat(result.scorePercent()).isEqualTo(70);
        verify(submissionRepository).saveScoredAnnotatedReview(
                eq(submissionId), any(), any(), eq(70), eq(70), eq(true));
        verify(notificationService).markStudentGradeUnseen(submissionId);
        verify(notificationService).markStudentFeedbackUnseen(submissionId);
    }

    @Test
    void saveFeedback_keepsEmojiInComment() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Mi respuesta", null, null, false));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED",
                        FormattedTextSegment.encodePlainFeedback("Bien 👏", 500), "ANNOTATED", 70, 70)));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("Bien 👏", annotated, null, 70, true));

        assertThat(result.feedbackText()).isEqualTo("Bien 👏");
        verify(submissionRepository).saveScoredAnnotatedReview(
                eq(submissionId), any(), any(), eq(70), eq(70), eq(true));
    }

    @Test
    void saveFeedback_acceptsEmojiOnlyComment() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Mi respuesta", null, null, false));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED",
                        FormattedTextSegment.encodePlainFeedback("👍", 500), "ANNOTATED", 100, 100)));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("👍", annotated, null, 100, true));

        assertThat(result.feedbackText()).isEqualTo("👍");
    }

    @Test
    void saveFeedback_rejectsWhenEmojiWouldExceedManualLimit() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));
        String tooLong = "a".repeat(FormattedTextSegment.MAX_MANUAL_FEEDBACK_LENGTH) + "👏";

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                review("Mi respuesta", tooLong, 100, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void saveFeedback_progressWriteStaysSubmitted() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Mi respuesta", null, null, false));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED",
                        FormattedTextSegment.encodePlainFeedback("Borrador", 500), "ANNOTATED", null, 40)));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("Borrador", annotated, null, 40, false));

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.scorePercent()).isNull();
        assertThat(result.teacherScorePercent()).isEqualTo(40);
        verify(submissionRepository).saveAnnotatedProgress(eq(submissionId), any(), any(), eq(40));
        verify(submissionRepository, never()).saveScoredAnnotatedReview(any(), any(), any(), anyInt(), any(), anyBoolean());
        verify(notificationService, never()).markStudentGradeUnseen(any());
        verify(notificationService, never()).markStudentFeedbackUnseen(any());
    }

    @Test
    void saveFeedback_finalizeWriteMissingPercentRejected() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("Bien",
                        List.of(new FormattedTextSegment("Mi respuesta", null, null, null)),
                        null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("puntuación");
        verify(submissionRepository, never()).saveScoredAnnotatedReview(any(), any(), any(), anyInt(), any(), anyBoolean());
    }

    @Test
    void saveFeedback_rejectsOutOfRangePercent() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Mi respuesta", null, 101, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 y 100");
    }

    @Test
    void saveFeedback_marksEachFreeTextAnswer() {
        UUID submissionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID assignmentId = stubManualFreeTextSubmission(submissionId, questionId);
        com.kuky.backend.learning.model.HomeworkAnswer answer =
                new com.kuky.backend.learning.model.HomeworkAnswer();
        answer.setId(answerId);
        answer.setQuestionId(questionId);
        answer.setAnswerText("Una respuesta");
        answer.setPromptSnapshot("¿Qué opinas?");
        answer.setScore(java.math.BigDecimal.ZERO);
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Una respuesta", null, "yellow", false));
        when(answerRepository.findBySubmission(submissionId))
                .thenReturn(List.of(answer))
                .thenReturn(List.of(scoredAnswer(answerId, questionId, 100)));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null, "MANUAL", "AUDIO")))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", null, "ANNOTATED", 100, null, "MANUAL", "AUDIO")));

        service.saveFeedback(submissionId, new SaveHomeworkFeedbackRequest(
                null, null, List.of(new SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest(
                        questionId, annotated, 100)), null, true));

        verify(answerRepository).updateManualReview(eq(answerId), any(), eq(100), any());
        verify(submissionRepository).saveScoredAnnotatedReview(eq(submissionId), isNull(), isNull(), eq(100), eq(true));
        verify(questionRepository).findByAssignment(assignmentId);
    }

    @Test
    void saveFeedback_freezesLegacyRichReview() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", "[]", "LEGACY_RICH")));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Mi respuesta", null, 100, true)))
                .isInstanceOf(AlreadyReviewedException.class);
    }

    @Test
    void saveFeedback_allowsAnnotatedReedit() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED",
                        FormattedTextSegment.encodePlainFeedback("Anterior", 500), "ANNOTATED", 100, 100)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED",
                        FormattedTextSegment.encodePlainFeedback("Actualizado", 500), "ANNOTATED", 80, 80)));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                review("Mi respuesta", "Actualizado", 80, true));

        assertThat(result.feedbackText()).isEqualTo("Actualizado");
        assertThat(result.scorePercent()).isEqualTo(80);
        verify(submissionRepository).saveScoredAnnotatedReview(
                eq(submissionId), any(), any(), eq(80), eq(80), eq(false));
        verify(notificationService).markStudentGradeUnseen(submissionId);
        verify(notificationService).markStudentFeedbackUnseen(submissionId);
    }

    @Test
    void saveFeedback_percentOnlyOnGraded_marksGradeUnseen() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        String feedbackJson = FormattedTextSegment.encodePlainFeedback("Igual", 500);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", feedbackJson, "ANNOTATED", 100, 100)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", feedbackJson, "ANNOTATED", 70, 70)));

        service.saveFeedback(submissionId, review("Mi respuesta", "Igual", 70, true));

        verify(notificationService).markStudentGradeUnseen(submissionId);
        verify(notificationService, never()).markStudentFeedbackUnseen(any());
        verify(notificationService, never()).markStudentFeedbackSeen(any());
    }

    @Test
    void saveFeedback_annotationOnlyOnGraded_marksFeedbackUnseen() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        String feedbackJson = FormattedTextSegment.encodePlainFeedback("Igual", 500);
        List<FormattedTextSegment> marked =
                List.of(new FormattedTextSegment("Mi respuesta", "red", "yellow", true));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", feedbackJson, "ANNOTATED", 100, 100)))
                .thenReturn(Optional.of(detailRowWithResponse(submissionId, "GRADED", feedbackJson, "ANNOTATED",
                        100, 100, FormattedTextSegment.toJson(marked))));

        service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("Igual", marked, null, 100, true));

        verify(notificationService, never()).markStudentGradeUnseen(any());
        verify(notificationService).markStudentFeedbackUnseen(submissionId);
        verify(notificationService, never()).markStudentFeedbackSeen(any());
    }

    @Test
    void saveFeedback_clearAllFeedbackOnGraded_marksFeedbackSeenNotGrade() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED",
                        FormattedTextSegment.encodePlainFeedback("Anterior", 500), "ANNOTATED", 100, 100)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", null, "ANNOTATED", 100, 100)));

        service.saveFeedback(submissionId, review("Mi respuesta", "   ", 100, true));

        verify(notificationService, never()).markStudentGradeUnseen(any());
        verify(notificationService, never()).markStudentFeedbackUnseen(any());
        verify(notificationService).markStudentFeedbackSeen(submissionId);
    }

    @Test
    void saveFeedback_identicalReedit_doesNotReNotify() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        String feedbackJson = FormattedTextSegment.encodePlainFeedback("Igual", 500);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", feedbackJson, "ANNOTATED", 80, 80)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", feedbackJson, "ANNOTATED", 80, 80)));

        service.saveFeedback(submissionId, review("Mi respuesta", "Igual", 80, true));

        verify(notificationService, never()).markStudentGradeUnseen(any());
        verify(notificationService, never()).markStudentFeedbackUnseen(any());
    }

    @Test
    void saveFeedback_rejectsFeedbackOverManualLimit() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                review("Mi respuesta", "a".repeat(FormattedTextSegment.MAX_MANUAL_FEEDBACK_LENGTH + 1), 100, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void saveFeedback_allowsEmptyFeedback() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "GRADED", null, "ANNOTATED", 100, 100)));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                review("Mi respuesta", "   ", 100, true));

        assertThat(result.feedbackText()).isNull();
        verify(submissionRepository).saveScoredAnnotatedReview(
                eq(submissionId), isNull(), any(), eq(100), eq(100), eq(true));
    }

    @Test
    void saveFeedback_notYetSubmitted_throws() {
        UUID submissionId = UUID.randomUUID();
        stubWriteSubmission(submissionId);
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "PENDING", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Mi respuesta", null, 100, true)))
                .isInstanceOf(NotSubmittedException.class);
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
        var notifications = mock(com.kuky.backend.notification.service.NotificationService.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class),
                audioFileRepository, userRepository, submissionRepository, grading, new ObjectMapper(),
                notifications,
                new com.kuky.backend.config.SchedulingProperties());

        var result = service.saveExerciseFeedback(submissionId, "  Muy bien  ");

        verify(submissionRepository).updateExerciseFeedback(eq(submissionId),
                eq(FormattedTextSegment.encodePlainFeedback("Muy bien")));
        verify(notifications).markStudentFeedbackUnseen(submissionId);
        assertThat(result.teacherFeedback()).isEqualTo("Muy bien");
    }

    @Test
    void saveExerciseFeedback_unchangedDoesNotNotify() {
        UUID submissionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        HomeworkSubmission graded = gradedSubmission(submissionId, assignmentId, "Muy bien");
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(graded));
        when(contentRepository.findAssignmentById(assignmentId))
                .thenReturn(Optional.of(exerciseAssignment(assignmentId)));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser()));
        ExerciseGradingService grading = mock(ExerciseGradingService.class);
        when(grading.viewGradedSubmission(any())).thenReturn(
                new ExerciseGradingService.GradedExerciseView(List.of(),
                        new com.kuky.backend.learning.dto.ExerciseResultResponse(100, 1, 1, List.of())));
        var notifications = mock(com.kuky.backend.notification.service.NotificationService.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class),
                audioFileRepository, userRepository, submissionRepository, grading, new ObjectMapper(),
                notifications,
                new com.kuky.backend.config.SchedulingProperties());

        service.saveExerciseFeedback(submissionId, "Muy bien");

        verify(notifications, never()).markStudentFeedbackUnseen(any());
        verify(notifications, never()).markStudentFeedbackSeen(any());
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
        var notifications = mock(com.kuky.backend.notification.service.NotificationService.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class),
                audioFileRepository, userRepository, submissionRepository, grading, new ObjectMapper(),
                notifications,
                new com.kuky.backend.config.SchedulingProperties());

        var result = service.saveExerciseFeedback(submissionId, "   ");

        verify(submissionRepository).updateExerciseFeedback(submissionId, null);
        verify(notifications).markStudentFeedbackSeen(submissionId);
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

    @Test
    void getSubmissionDetail_includesFreeTextAnswersWithSnapshots() {
        UUID submissionId = UUID.randomUUID();
        UUID qid = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId)).thenReturn(Optional.of(
                new HomeworkSubmissionRepository.SubmissionDetailRow(
                        submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                        "Escucha", "SUBMITTED", null, null, null, null, null, "MANUAL", "AUDIO", Instant.now(), null)));
        com.kuky.backend.learning.repository.HomeworkAnswerRepository answers =
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class);
        com.kuky.backend.learning.model.HomeworkAnswer row = new com.kuky.backend.learning.model.HomeworkAnswer();
        row.setQuestionId(qid);
        row.setPromptSnapshot("¿Qué oyes?");
        row.setAnswerText("una noticia");
        when(answers.findBySubmission(submissionId)).thenReturn(List.of(row));
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                answers, audioFileRepository, userRepository, submissionRepository,
                mock(ExerciseGradingService.class), new ObjectMapper(),
                mock(com.kuky.backend.notification.service.NotificationService.class),
                new com.kuky.backend.config.SchedulingProperties());

        HomeworkSubmissionAdminDto detail = service.getSubmissionDetail(submissionId);

        assertThat(detail.answers()).hasSize(1);
        assertThat(detail.answers().get(0).promptSnapshot()).isEqualTo("¿Qué oyes?");
        assertThat(detail.answers().get(0).text()).isEqualTo("una noticia");
        assertThat(detail.response()).isNull();
    }

    @Test
    void validateAndMapQuestions_writeRejectsQuestions() {
        assertThatThrownBy(() -> service.validateAndMapQuestions(
                true,
                List.of(new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "FREE_TEXT", "¿Hola?", List.of(), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escritura");
    }

    @Test
    void validateAndMapQuestions_nonWriteRequiresAtLeastOne() {
        assertThatThrownBy(() -> service.validateAndMapQuestions(false, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una pregunta");
    }

    @Test
    void validateAndMapQuestions_allowsMixedKinds() {
        var mapped = service.validateAndMapQuestions(false, List.of(
                new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "FREE_TEXT", "¿Qué oyes?", List.of(), null),
                new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "SINGLE_CHOICE", "¿Tema?",
                        List.of(
                                new com.kuky.backend.admin.dto.HomeworkQuestionDto.OptionDto(null, "a", true),
                                new com.kuky.backend.admin.dto.HomeworkQuestionDto.OptionDto(null, "b", false)),
                        null)));
        assertThat(mapped).hasSize(2);
        assertThat(mapped.get(0).getKind()).isEqualTo(com.kuky.backend.learning.model.QuestionKind.FREE_TEXT);
        assertThat(mapped.get(1).getKind()).isEqualTo(com.kuky.backend.learning.model.QuestionKind.SINGLE_CHOICE);
    }

    @Test
    void validateAndMapQuestions_preservesQuestionAndOptionIds() {
        UUID questionId = UUID.randomUUID();
        UUID optionA = UUID.randomUUID();
        UUID optionB = UUID.randomUUID();
        var mapped = service.validateAndMapQuestions(false, List.of(
                new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        questionId, "SINGLE_CHOICE", "¿Tema?",
                        List.of(
                                new com.kuky.backend.admin.dto.HomeworkQuestionDto.OptionDto(optionA, "a", true),
                                new com.kuky.backend.admin.dto.HomeworkQuestionDto.OptionDto(optionB, "b", false)),
                        null)));
        assertThat(mapped).hasSize(1);
        assertThat(mapped.get(0).getId()).isEqualTo(questionId);
        assertThat(mapped.get(0).getOptions()).hasSize(2);
        assertThat(mapped.get(0).getOptions().get(0).getId()).isEqualTo(optionA);
        assertThat(mapped.get(0).getOptions().get(1).getId()).isEqualTo(optionB);
    }

    @Test
    void validateAndMapQuestions_manualAudioRequiresFreeText() {
        var mapped = service.validateAndMapQuestions(
                false,
                List.of(new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "FREE_TEXT", "¿Qué oyes?", List.of(), null)));
        assertThat(mapped).hasSize(1);
        assertThat(mapped.get(0).getKind()).isEqualTo(com.kuky.backend.learning.model.QuestionKind.FREE_TEXT);
    }

    private void stubWriteSubmission(UUID submissionId) {
        UUID assignmentId = UUID.randomUUID();
        HomeworkSubmission sub = new HomeworkSubmission();
        sub.setId(submissionId);
        sub.setAssignmentId(assignmentId);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sub));
        HomeworkAssignment a = assignment(assignmentId);
        a.setHomeworkType(com.kuky.backend.learning.model.HomeworkType.WRITE);
        a.setFormat(com.kuky.backend.learning.model.HomeworkFormat.MANUAL);
        when(contentRepository.findAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(questionRepository.findByAssignment(assignmentId)).thenReturn(List.of());
    }

    private UUID stubManualFreeTextSubmission(UUID submissionId, UUID questionId) {
        UUID assignmentId = UUID.randomUUID();
        HomeworkSubmission sub = new HomeworkSubmission();
        sub.setId(submissionId);
        sub.setAssignmentId(assignmentId);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sub));
        HomeworkAssignment a = assignment(assignmentId);
        a.setHomeworkType(com.kuky.backend.learning.model.HomeworkType.AUDIO);
        a.setFormat(com.kuky.backend.learning.model.HomeworkFormat.MANUAL);
        when(contentRepository.findAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        com.kuky.backend.learning.model.HomeworkQuestion q = new com.kuky.backend.learning.model.HomeworkQuestion();
        q.setId(questionId);
        q.setKind(com.kuky.backend.learning.model.QuestionKind.FREE_TEXT);
        q.setPrompt("¿Qué opinas?");
        when(questionRepository.findByAssignment(assignmentId)).thenReturn(List.of(q));
        return assignmentId;
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

    private SaveHomeworkFeedbackRequest review(String text, String feedbackText, Integer teacherScorePercent,
                                               Boolean finalize) {
        return new SaveHomeworkFeedbackRequest(feedbackText,
                List.of(new FormattedTextSegment(text, null, null, null)), null,
                teacherScorePercent, finalize);
    }

    private static com.kuky.backend.learning.model.HomeworkAnswer scoredAnswer(
            UUID answerId, UUID questionId, int percent) {
        com.kuky.backend.learning.model.HomeworkAnswer a = new com.kuky.backend.learning.model.HomeworkAnswer();
        a.setId(answerId);
        a.setQuestionId(questionId);
        a.setAnswerText("Una respuesta");
        a.setPromptSnapshot("¿Qué opinas?");
        a.setTeacherScorePercent(percent);
        a.setScore(java.math.BigDecimal.valueOf(percent / 100.0).setScale(3, java.math.RoundingMode.HALF_UP));
        return a;
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRow(UUID submissionId, String status,
                                                                       String feedbackJson, String reviewModel) {
        return detailRow(submissionId, status, feedbackJson, reviewModel,
                "GRADED".equals(status) ? 100 : null,
                "GRADED".equals(status) ? 100 : null,
                "MANUAL", "WRITE");
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRow(UUID submissionId, String status,
                                                                       String feedbackJson, String reviewModel,
                                                                       Integer scorePercent, Integer teacherScorePercent) {
        return detailRow(submissionId, status, feedbackJson, reviewModel, scorePercent, teacherScorePercent,
                "MANUAL", "WRITE");
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRow(UUID submissionId, String status,
                                                                       String feedbackJson, String reviewModel,
                                                                       String format, String homeworkType) {
        return detailRow(submissionId, status, feedbackJson, reviewModel,
                "GRADED".equals(status) ? 100 : null, null, format, homeworkType);
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRowWithResponse(
            UUID submissionId, String status, String feedbackJson, String reviewModel,
            Integer scorePercent, Integer teacherScorePercent, String response) {
        return new HomeworkSubmissionRepository.SubmissionDetailRow(
                submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                "Tarea", status, response, feedbackJson, reviewModel, scorePercent, teacherScorePercent,
                "MANUAL", "WRITE", Instant.now(), Instant.now());
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRow(UUID submissionId, String status,
                                                                       String feedbackJson, String reviewModel,
                                                                       Integer scorePercent, Integer teacherScorePercent,
                                                                       String format, String homeworkType) {
        String response = FormattedTextSegment.toJson(List.of(new FormattedTextSegment("Mi respuesta", null, null, null)));
        return new HomeworkSubmissionRepository.SubmissionDetailRow(
                submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                "Tarea", status, response, feedbackJson, reviewModel, scorePercent, teacherScorePercent,
                format, homeworkType, Instant.now(),
                ("REVIEWED".equals(status) || "GRADED".equals(status)) ? Instant.now() : null);
    }
}
