package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.StudentProfileResponse;
import com.kuky.backend.admin.service.StudentProfileAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.units.repository.UnitRepository;
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
    private UnitRepository unitRepository;
    private StudentProfileAdminService service;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        homeworkTargetRepository = mock(HomeworkTargetRepository.class);
        presentationRepository = mock(PresentationRepository.class);
        unitRepository = mock(UnitRepository.class);
        service = new StudentProfileAdminService(userRepository, bookingRepository,
                homeworkTargetRepository, presentationRepository, unitRepository);

        User student = new User();
        student.setId(studentId);
        student.setEmail("ana@example.com");
        student.setRole("STUDENT");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(presentationRepository.findSharedSummariesForUser(studentId)).thenReturn(List.of());
        when(unitRepository.findProgressForStudent(studentId)).thenReturn(List.of());
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

    private HomeworkTargetRepository.StudentAssignmentView homework(String status) {
        return new HomeworkTargetRepository.StudentAssignmentView(UUID.randomUUID(), "Tarea", status, null, "MANUAL", UUID.randomUUID());
    }

    @Test
    void emptyStudentGetsAllZeroProgress() {
        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.progress().units()).isEmpty();
        assertThat(response.progress().homeworkBreakdown().pending()).isZero();
        assertThat(response.progress().homeworkBreakdown().submitted()).isZero();
        assertThat(response.progress().homeworkBreakdown().completed()).isZero();
        assertThat(response.progress().attendedClasses()).isZero();
    }

    @Test
    void homeworkBreakdownGroupsReviewedAndGradedTogether() {
        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                homework("PENDING"),
                homework("SUBMITTED"),
                homework("REVIEWED"),
                homework("GRADED")));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.progress().homeworkBreakdown().pending()).isEqualTo(1);
        assertThat(response.progress().homeworkBreakdown().submitted()).isEqualTo(1);
        assertThat(response.progress().homeworkBreakdown().completed()).isEqualTo(2);
    }

    @Test
    void homeworkStatusTransitionMovesItBetweenBreakdownBucketsOnNextCall() {
        UUID assignmentId = UUID.randomUUID();
        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                new HomeworkTargetRepository.StudentAssignmentView(assignmentId, "Tarea", "SUBMITTED", null, "MANUAL", UUID.randomUUID())));

        StudentProfileResponse before = service.getProfile(studentId);
        assertThat(before.progress().homeworkBreakdown().submitted()).isEqualTo(1);
        assertThat(before.progress().homeworkBreakdown().completed()).isZero();

        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                new HomeworkTargetRepository.StudentAssignmentView(assignmentId, "Tarea", "REVIEWED", Instant.now(), "MANUAL", UUID.randomUUID())));

        StudentProfileResponse after = service.getProfile(studentId);
        assertThat(after.progress().homeworkBreakdown().submitted()).isZero();
        assertThat(after.progress().homeworkBreakdown().completed()).isEqualTo(1);
    }

    @Test
    void needsReview_isTrueOnlyForManualSubmittedHomework() {
        UUID manualSubmitted = UUID.randomUUID();
        UUID manualPending = UUID.randomUUID();
        UUID manualReviewed = UUID.randomUUID();
        UUID exerciseSubmittedEquivalent = UUID.randomUUID();
        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                new HomeworkTargetRepository.StudentAssignmentView(manualSubmitted, "Escritura", "SUBMITTED", Instant.now(), "MANUAL", UUID.randomUUID()),
                new HomeworkTargetRepository.StudentAssignmentView(manualPending, "Escritura 2", "PENDING", null, "MANUAL", null),
                new HomeworkTargetRepository.StudentAssignmentView(manualReviewed, "Escritura 3", "REVIEWED", Instant.now(), "MANUAL", UUID.randomUUID()),
                new HomeworkTargetRepository.StudentAssignmentView(exerciseSubmittedEquivalent, "Ejercicio", "GRADED", Instant.now(), "EXERCISE", UUID.randomUUID())));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualSubmitted)).findFirst().orElseThrow().needsReview()).isTrue();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualPending)).findFirst().orElseThrow().needsReview()).isFalse();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(manualReviewed)).findFirst().orElseThrow().needsReview()).isFalse();
        assertThat(response.homeworks().stream()
                .filter(h -> h.id().equals(exerciseSubmittedEquivalent)).findFirst().orElseThrow().needsReview()).isFalse();
    }

    @Test
    void attendedClassesCountsOnlyPastConfirmedBookings() {
        Instant now = Instant.now();
        when(bookingRepository.findByUserId(studentId)).thenReturn(List.of(
                booking("CONFIRMED", now.minus(7, ChronoUnit.DAYS)),
                booking("CONFIRMED", now.plus(7, ChronoUnit.DAYS)),
                booking("CANCELLED", now.minus(7, ChronoUnit.DAYS))));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.progress().attendedClasses()).isEqualTo(1);
    }

    @Test
    void booking_reportsThisStudentsOwnNoShowFlag_whenViewedStudentIsTheSecondStudent() {
        // findByUserId matches both the primary and second student on a shared booking (spec 021);
        // this profile belongs to the second student, so it must report second_student_no_show,
        // not the primary's no_show flag — regardless of which one is actually true.
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

    @Test
    void unitIsCompleteOnlyWhenAllTargetedHomeworksAreDone() {
        UUID doneUnit = UUID.randomUUID();
        UUID inProgressUnit = UUID.randomUUID();
        UUID emptyUnit = UUID.randomUUID();
        when(unitRepository.findProgressForStudent(studentId)).thenReturn(List.of(
                new UnitRepository.UnitProgressView(doneUnit, "Gramática", "B1", 2, 2),
                new UnitRepository.UnitProgressView(inProgressUnit, "Vocabulario", "B1", 3, 1),
                new UnitRepository.UnitProgressView(emptyUnit, "Fonética", "A2", 0, 0)));

        StudentProfileResponse response = service.getProfile(studentId);

        assertThat(response.progress().units()).hasSize(3);
        assertThat(response.progress().units().stream()
                .filter(u -> u.unitId().equals(doneUnit)).findFirst().orElseThrow().complete()).isTrue();
        assertThat(response.progress().units().stream()
                .filter(u -> u.unitId().equals(inProgressUnit)).findFirst().orElseThrow().complete()).isFalse();
        assertThat(response.progress().units().stream()
                .filter(u -> u.unitId().equals(emptyUnit)).findFirst().orElseThrow().complete()).isFalse();
    }
}
