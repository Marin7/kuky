package com.kuky.backend.learning;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.PastClass;
import com.kuky.backend.learning.model.PresentationBlock;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.service.LearningService;
import com.kuky.backend.presentations.repository.PresentationRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private HomeworkSubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PresentationRepository presentationRepository;

    private LearningService service;

    private final UUID userId = UUID.randomUUID();
    private static final String EMAIL = "student@example.com";

    @BeforeEach
    void setUp() {
        service = new LearningService(contentRepository, submissionRepository, userRepository,
                presentationRepository, new SchedulingProperties());
        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(presentationRepository.findSharedSummariesForUser(userId)).thenReturn(List.of());
    }

    @Test
    void getOverview_mapsPresentationAndPastClasses() {
        when(contentRepository.findPublishedPresentation()).thenReturn(List.of(
                presentation("Hola", "Bienvenido")));
        when(contentRepository.findPublishedPastClassesSince(any(LocalDate.class))).thenReturn(List.of(
                pastClass("El indefinido", LocalDate.of(2026, 6, 3))));
        when(contentRepository.findAssignmentsForUser(userId)).thenReturn(List.of());
        when(submissionRepository.findByUserId(userId)).thenReturn(List.of());

        LearningResponse overview = service.getOverview(EMAIL);

        assertThat(overview.presentation()).hasSize(1);
        assertThat(overview.presentation().get(0).heading()).isEqualTo("Hola");
        assertThat(overview.pastClasses()).hasSize(1);
        assertThat(overview.pastClasses().get(0).title()).isEqualTo("El indefinido");
        assertThat(overview.homework()).isEmpty();
    }

    @Test
    void getOverview_assignmentWithoutSubmission_isPending() {
        HomeworkAssignment a = assignment("Tarea 1", LocalDate.now().plusDays(5));
        when(contentRepository.findPublishedPresentation()).thenReturn(List.of());
        when(contentRepository.findPublishedPastClassesSince(any(LocalDate.class))).thenReturn(List.of());
        when(contentRepository.findAssignmentsForUser(userId)).thenReturn(List.of(a));
        when(submissionRepository.findByUserId(userId)).thenReturn(List.of());

        LearningResponse overview = service.getOverview(EMAIL);

        assertThat(overview.homework()).hasSize(1);
        assertThat(overview.homework().get(0).status()).isEqualTo("PENDING");
        assertThat(overview.homework().get(0).overdue()).isFalse();
        assertThat(overview.homework().get(0).response()).isNull();
    }

    @Test
    void getOverview_pendingPastDue_isOverdue() {
        HomeworkAssignment a = assignment("Tarea atrasada", LocalDate.now().minusDays(1));
        when(contentRepository.findPublishedPresentation()).thenReturn(List.of());
        when(contentRepository.findPublishedPastClassesSince(any(LocalDate.class))).thenReturn(List.of());
        when(contentRepository.findAssignmentsForUser(userId)).thenReturn(List.of(a));
        when(submissionRepository.findByUserId(userId)).thenReturn(List.of());

        LearningResponse overview = service.getOverview(EMAIL);

        assertThat(overview.homework().get(0).overdue()).isTrue();
    }

    @Test
    void getOverview_noDueDate_neverOverdue() {
        HomeworkAssignment a = assignment("Lectura libre", null);
        when(contentRepository.findPublishedPresentation()).thenReturn(List.of());
        when(contentRepository.findPublishedPastClassesSince(any(LocalDate.class))).thenReturn(List.of());
        when(contentRepository.findAssignmentsForUser(userId)).thenReturn(List.of(a));
        when(submissionRepository.findByUserId(userId)).thenReturn(List.of());

        LearningResponse overview = service.getOverview(EMAIL);

        assertThat(overview.homework().get(0).overdue()).isFalse();
    }

    @Test
    void getOverview_withSubmission_reflectsStatusAndNotOverdue() {
        HomeworkAssignment a = assignment("Tarea enviada", LocalDate.now().minusDays(1));
        HomeworkSubmission s = new HomeworkSubmission();
        s.setAssignmentId(a.getId());
        s.setUserId(userId);
        s.setStatus(HomeworkStatus.SUBMITTED.name());
        s.setResponseText("Mi respuesta");
        s.setSubmittedAt(Instant.now());
        when(contentRepository.findPublishedPresentation()).thenReturn(List.of());
        when(contentRepository.findPublishedPastClassesSince(any(LocalDate.class))).thenReturn(List.of());
        when(contentRepository.findAssignmentsForUser(userId)).thenReturn(List.of(a));
        when(submissionRepository.findByUserId(userId)).thenReturn(List.of(s));

        LearningResponse overview = service.getOverview(EMAIL);

        assertThat(overview.homework().get(0).status()).isEqualTo("SUBMITTED");
        assertThat(overview.homework().get(0).response()).isEqualTo("Mi respuesta");
        // Past-due but submitted ⇒ not overdue
        assertThat(overview.homework().get(0).overdue()).isFalse();
    }

    // ---- helpers ----

    private PresentationBlock presentation(String heading, String body) {
        PresentationBlock p = new PresentationBlock();
        p.setId(UUID.randomUUID());
        p.setHeading(heading);
        p.setBody(body);
        p.setPublished(true);
        return p;
    }

    private PastClass pastClass(String title, LocalDate heldOn) {
        PastClass c = new PastClass();
        c.setId(UUID.randomUUID());
        c.setTitle(title);
        c.setHeldOn(heldOn);
        c.setTeacherNote("Nota");
        c.setPublished(true);
        return c;
    }

    private HomeworkAssignment assignment(String title, LocalDate dueOn) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(UUID.randomUUID());
        a.setTitle(title);
        a.setInstructions("Instrucciones");
        a.setDueOn(dueOn);
        a.setPublished(true);
        return a;
    }
}
