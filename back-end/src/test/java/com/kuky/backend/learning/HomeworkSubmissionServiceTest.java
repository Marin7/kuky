package com.kuky.backend.learning;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.service.HomeworkSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkSubmissionServiceTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private HomeworkSubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;

    private HomeworkSubmissionService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private static final String EMAIL = "student@example.com";

    @BeforeEach
    void setUp() {
        service = new HomeworkSubmissionService(contentRepository, submissionRepository, userRepository,
                new SchedulingProperties());
        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void submit_newSubmission_setsSubmittedWithResponse() {
        HomeworkAssignment a = assignment(LocalDate.now().minusDays(1));
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), eq("Mi respuesta"), any()))
                .thenReturn(submission(HomeworkStatus.SUBMITTED, "Mi respuesta"));

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId, "Mi respuesta");

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.response()).isEqualTo("Mi respuesta");
        // Past-due but now submitted ⇒ not overdue
        assertThat(result.overdue()).isFalse();
    }

    @Test
    void submit_markDoneWithoutText_setsSubmitted() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId)).thenReturn(Optional.empty());
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), eq(null), any()))
                .thenReturn(submission(HomeworkStatus.SUBMITTED, null));

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId, null);

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.response()).isNull();
    }

    @Test
    void submit_unknownAssignment_throwsNotFound() {
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId, "x"))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void submit_whenReviewed_throwsNotAllowed() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId))
                .thenReturn(Optional.of(submission(HomeworkStatus.REVIEWED, "ya revisada")));

        assertThatThrownBy(() -> service.submit(EMAIL, assignmentId, "nuevo intento"))
                .isInstanceOf(SubmissionNotAllowedException.class);
    }

    @Test
    void submit_whenAlreadySubmitted_isIdempotentUpdate() {
        HomeworkAssignment a = assignment(null);
        when(contentRepository.findPublishedAssignmentById(assignmentId)).thenReturn(Optional.of(a));
        when(submissionRepository.findByUserAndAssignment(userId, assignmentId))
                .thenReturn(Optional.of(submission(HomeworkStatus.SUBMITTED, "anterior")));
        when(submissionRepository.upsert(eq(userId), eq(assignmentId),
                eq(HomeworkStatus.SUBMITTED.name()), eq("actualizada"), any()))
                .thenReturn(submission(HomeworkStatus.SUBMITTED, "actualizada"));

        HomeworkItemResponse result = service.submit(EMAIL, assignmentId, "actualizada");

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.response()).isEqualTo("actualizada");
    }

    // ---- helpers ----

    private HomeworkAssignment assignment(LocalDate dueOn) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(assignmentId);
        a.setTitle("Tarea");
        a.setInstructions("Instrucciones");
        a.setDueOn(dueOn);
        a.setPublished(true);
        return a;
    }

    private HomeworkSubmission submission(HomeworkStatus status, String response) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setUserId(userId);
        s.setAssignmentId(assignmentId);
        s.setStatus(status.name());
        s.setResponseText(response);
        s.setSubmittedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }
}
