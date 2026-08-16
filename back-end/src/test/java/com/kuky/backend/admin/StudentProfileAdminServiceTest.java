package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.StudentProfileResponse;
import com.kuky.backend.admin.service.StudentProfileAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentProfileAdminServiceTest {

    private UserRepository userRepository;
    private BookingRepository bookingRepository;
    private HomeworkTargetRepository homeworkTargetRepository;
    private PresentationRepository presentationRepository;
    private StudentProfileAdminService service;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        homeworkTargetRepository = mock(HomeworkTargetRepository.class);
        presentationRepository = mock(PresentationRepository.class);
        service = new StudentProfileAdminService(userRepository, bookingRepository,
                homeworkTargetRepository, presentationRepository, new com.kuky.backend.config.SchedulingProperties());

        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(presentationRepository.findSharedSummariesForUser(studentId)).thenReturn(List.of());
        when(bookingRepository.findByUserId(studentId)).thenReturn(List.of());
        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of());
    }

    private Booking booking(String status, Instant slotStart) {
        Booking b = new Booking();
        b.setId(UUID.randomUUID());
        b.setUserId(studentId);
        b.setSlotStart(slotStart);
        b.setDurationMinutes(50);
        b.setStatus(status);
        return b;
    }

    @Test
    void emptyStudentGetsEmptyLists() {
        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.homeworks()).isEmpty();
        assertThat(response.bookings()).isEmpty();
        assertThat(response.presentations()).isEmpty();
        assertThat(response.email()).isEqualTo("ana@example.com");
    }

    @Test
    void profileReturnsAllHomeworkStatusesWithoutProgressAggregate() {
        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                new HomeworkTargetRepository.StudentAssignmentView(
                        UUID.randomUUID(), "Tarea 1", "PENDING", null, "MANUAL", null, null, false, false, null),
                new HomeworkTargetRepository.StudentAssignmentView(
                        UUID.randomUUID(), "Tarea 2", "SUBMITTED", Instant.now(), "MANUAL", UUID.randomUUID(), null, false, false, null),
                new HomeworkTargetRepository.StudentAssignmentView(
                        UUID.randomUUID(), "Tarea 3", "REVIEWED", Instant.now(), "MANUAL", UUID.randomUUID(), null, false, false, null),
                new HomeworkTargetRepository.StudentAssignmentView(
                        UUID.randomUUID(), "Tarea 4", "GRADED", Instant.now(), "EXERCISE", UUID.randomUUID(), 90, false, false, null)));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.homeworks()).hasSize(4);
        assertThat(response.homeworks().stream().map(h -> h.status()).toList())
                .containsExactlyInAnyOrder("PENDING", "SUBMITTED", "REVIEWED", "GRADED");
    }

    @Test
    void needsReview_isTrueOnlyForManualSubmittedHomework() {
        UUID manualSubmitted = UUID.randomUUID();
        UUID manualPending = UUID.randomUUID();
        UUID manualReviewed = UUID.randomUUID();
        UUID exerciseSubmittedEquivalent = UUID.randomUUID();
        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                new HomeworkTargetRepository.StudentAssignmentView(manualSubmitted, "Escritura", "SUBMITTED", Instant.now(), "MANUAL", UUID.randomUUID(), null, false, false, null),
                new HomeworkTargetRepository.StudentAssignmentView(manualPending, "Escritura 2", "PENDING", null, "MANUAL", null, null, false, false, java.time.LocalDate.now().minusDays(1)),
                new HomeworkTargetRepository.StudentAssignmentView(manualReviewed, "Escritura 3", "REVIEWED", Instant.now(), "MANUAL", UUID.randomUUID(), null, false, false, null),
                new HomeworkTargetRepository.StudentAssignmentView(exerciseSubmittedEquivalent, "Ejercicio", "GRADED", Instant.now(), "EXERCISE", UUID.randomUUID(), 80, false, false, null)));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualSubmitted)).findFirst().orElseThrow().needsReview()).isTrue();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualPending)).findFirst().orElseThrow().needsReview()).isFalse();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualReviewed)).findFirst().orElseThrow().needsReview()).isFalse();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(exerciseSubmittedEquivalent)).findFirst().orElseThrow().needsReview()).isFalse();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualPending)).findFirst().orElseThrow().overdue()).isTrue();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualSubmitted)).findFirst().orElseThrow().overdue()).isFalse();
    }

    @Test
    void booking_reportsThisStudentsOwnNoShowFlag_whenViewedStudentIsTheSecondStudent() {
        UUID primaryId = UUID.randomUUID();
        Booking shared = new Booking();
        shared.setId(UUID.randomUUID());
        shared.setUserId(primaryId);
        shared.setCompanionStudentId(studentId);
        shared.setSlotStart(Instant.now().minus(1, ChronoUnit.DAYS));
        shared.setDurationMinutes(60);
        shared.setStatus("CONFIRMED");
        shared.setNoShow(true);
        shared.setCompanionStudentNoShow(false);
        when(bookingRepository.findByUserId(studentId)).thenReturn(List.of(shared));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.bookings()).hasSize(1);
        assertThat(response.bookings().get(0).isCompanionStudent()).isTrue();
        assertThat(response.bookings().get(0).noShow()).isFalse();
    }

    @Test
    void booking_reportsPrimaryNoShowFlag_whenViewedStudentIsThePrimary() {
        Booking own = booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));
        own.setNoShow(true);
        when(bookingRepository.findByUserId(studentId)).thenReturn(List.of(own));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.bookings().get(0).isCompanionStudent()).isFalse();
        assertThat(response.bookings().get(0).noShow()).isTrue();
    }
}
