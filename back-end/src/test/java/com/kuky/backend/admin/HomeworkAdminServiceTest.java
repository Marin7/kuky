package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
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
        answerRepository = mock(HomeworkAnswerRepository.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                answerRepository,
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
                "Tarea", "Hazla", LocalDate.of(2026, 6, 20), "WRITE", null, "MANUAL", List.of(), null, null, List.of(studentId)));

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
                "Tarea", "Hazla", null, "WRITE", null, "MANUAL", List.of(), null, null, List.of(unknown))))
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
                new com.kuky.backend.admin.dto.UpdateHomeworkRequest("T", "I", null, "WRITE", null, "MANUAL", List.of(), null, null)))
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
    void saveFeedback_rejectsChangedStudentWording() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Texto cambiado", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("respuesta del alumno");
        verify(submissionRepository, never()).saveAnnotatedReview(any(), any(), any(), anyBoolean());
    }

    @Test
    void saveFeedback_marksWriteAnswerAndTransitionsToAnnotated() {
        UUID submissionId = UUID.randomUUID();
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Mi respuesta", "red", "yellow", true));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED",
                        FormattedTextSegment.encodePlainFeedback("Bien", 500), "ANNOTATED")));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId,
                new SaveHomeworkFeedbackRequest("Bien", annotated, null));

        assertThat(result.reviewModel()).isEqualTo("ANNOTATED");
        assertThat(result.feedbackText()).isEqualTo("Bien");
        verify(submissionRepository).saveAnnotatedReview(eq(submissionId), any(), any(), eq(true));
    }

    @Test
    void saveFeedback_marksEachFreeTextAnswer() {
        UUID submissionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        com.kuky.backend.learning.model.HomeworkAnswer answer =
                new com.kuky.backend.learning.model.HomeworkAnswer();
        answer.setId(answerId);
        answer.setQuestionId(questionId);
        answer.setAnswerText("Una respuesta");
        answer.setPromptSnapshot("¿Qué opinas?");
        List<FormattedTextSegment> annotated =
                List.of(new FormattedTextSegment("Una respuesta", null, "yellow", false));
        when(answerRepository.findBySubmission(submissionId)).thenReturn(List.of(answer));
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", null, "ANNOTATED")));

        service.saveFeedback(submissionId, new SaveHomeworkFeedbackRequest(
                null, null, List.of(new SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest(questionId, annotated))));

        verify(answerRepository).updateAnswerText(answerId, FormattedTextSegment.toJson(annotated));
        verify(submissionRepository).saveAnnotatedReview(eq(submissionId), isNull(), isNull(), eq(true));
    }

    @Test
    void saveFeedback_freezesLegacyRichReview() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", "[]", "LEGACY_RICH")));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Mi respuesta", null)))
                .isInstanceOf(AlreadyReviewedException.class);
    }

    @Test
    void saveFeedback_allowsAnnotatedReedit() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED",
                        FormattedTextSegment.encodePlainFeedback("Anterior", 500), "ANNOTATED")))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED",
                        FormattedTextSegment.encodePlainFeedback("Actualizado", 500), "ANNOTATED")));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId, review("Mi respuesta", "Actualizado"));

        assertThat(result.feedbackText()).isEqualTo("Actualizado");
        verify(submissionRepository).saveAnnotatedReview(eq(submissionId), any(), any(), eq(false));
    }

    @Test
    void saveFeedback_rejectsFeedbackOverManualLimit() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId,
                review("Mi respuesta", "a".repeat(FormattedTextSegment.MAX_MANUAL_FEEDBACK_LENGTH + 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void saveFeedback_allowsEmptyFeedback() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "SUBMITTED", null, null)))
                .thenReturn(Optional.of(detailRow(submissionId, "REVIEWED", null, "ANNOTATED")));

        HomeworkSubmissionAdminDto result = service.saveFeedback(submissionId, review("Mi respuesta", "   "));

        assertThat(result.feedbackText()).isNull();
        verify(submissionRepository).saveAnnotatedReview(eq(submissionId), isNull(), any(), eq(true));
    }

    @Test
    void saveFeedback_notYetSubmitted_throws() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId))
                .thenReturn(Optional.of(detailRow(submissionId, "PENDING", null, null)));

        assertThatThrownBy(() -> service.saveFeedback(submissionId, review("Mi respuesta", null)))
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
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class),
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
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class),
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

    @Test
    void getSubmissionDetail_includesFreeTextAnswersWithSnapshots() {
        UUID submissionId = UUID.randomUUID();
        UUID qid = UUID.randomUUID();
        when(submissionRepository.findDetailById(submissionId)).thenReturn(Optional.of(
                new HomeworkSubmissionRepository.SubmissionDetailRow(
                        submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                        "Escucha", "SUBMITTED", null, null, null, Instant.now(), null)));
        com.kuky.backend.learning.repository.HomeworkAnswerRepository answers =
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class);
        com.kuky.backend.learning.model.HomeworkAnswer row = new com.kuky.backend.learning.model.HomeworkAnswer();
        row.setQuestionId(qid);
        row.setPromptSnapshot("¿Qué oyes?");
        row.setAnswerText("una noticia");
        when(answers.findBySubmission(submissionId)).thenReturn(List.of(row));
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                answers, audioFileRepository, userRepository, submissionRepository,
                mock(ExerciseGradingService.class), new ObjectMapper());

        HomeworkSubmissionAdminDto detail = service.getSubmissionDetail(submissionId);

        assertThat(detail.answers()).hasSize(1);
        assertThat(detail.answers().get(0).promptSnapshot()).isEqualTo("¿Qué oyes?");
        assertThat(detail.answers().get(0).text()).isEqualTo("una noticia");
        assertThat(detail.response()).isNull();
    }

    @Test
    void validateAndMapQuestions_manualWriteRejectsQuestions() {
        assertThatThrownBy(() -> service.validateAndMapQuestions(
                com.kuky.backend.learning.model.HomeworkFormat.MANUAL,
                List.of(new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "FREE_TEXT", "¿Hola?", List.of(), null)),
                false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escritura");
    }

    @Test
    void validateAndMapQuestions_manualAudioRequiresFreeText() {
        var mapped = service.validateAndMapQuestions(
                com.kuky.backend.learning.model.HomeworkFormat.MANUAL,
                List.of(new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "FREE_TEXT", "¿Qué oyes?", List.of(), null)),
                true);
        assertThat(mapped).hasSize(1);
        assertThat(mapped.get(0).getKind()).isEqualTo(com.kuky.backend.learning.model.QuestionKind.FREE_TEXT);
    }

    @Test
    void validateAndMapQuestions_exerciseRejectsFreeText() {
        assertThatThrownBy(() -> service.validateAndMapQuestions(
                com.kuky.backend.learning.model.HomeworkFormat.EXERCISE,
                List.of(new com.kuky.backend.admin.dto.HomeworkQuestionDto(
                        null, "FREE_TEXT", "¿…?", List.of(), null)),
                false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("autocorregibles");
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

    private SaveHomeworkFeedbackRequest review(String text, String feedbackText) {
        return new SaveHomeworkFeedbackRequest(feedbackText,
                List.of(new FormattedTextSegment(text, null, null, null)), null);
    }

    private HomeworkSubmissionRepository.SubmissionDetailRow detailRow(UUID submissionId, String status,
                                                                       String feedbackJson, String reviewModel) {
        String response = FormattedTextSegment.toJson(List.of(new FormattedTextSegment("Mi respuesta", null, null, null)));
        return new HomeworkSubmissionRepository.SubmissionDetailRow(
                submissionId, studentId, "ana@example.com", "Ana", "Lopez", null,
                "Tarea", status, response, feedbackJson, reviewModel, Instant.now(),
                "REVIEWED".equals(status) ? Instant.now() : null);
    }
}
