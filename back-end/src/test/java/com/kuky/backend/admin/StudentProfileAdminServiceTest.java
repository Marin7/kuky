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
        return new HomeworkTargetRepository.StudentAssignmentView(UUID.randomUUID(), "Tarea", status, null);
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
                new HomeworkTargetRepository.StudentAssignmentView(assignmentId, "Tarea", "SUBMITTED", null)));

        StudentProfileResponse before = service.getProfile(studentId);
        assertThat(before.progress().homeworkBreakdown().submitted()).isEqualTo(1);
        assertThat(before.progress().homeworkBreakdown().completed()).isZero();

        when(homeworkTargetRepository.findAssignmentsForStudent(studentId)).thenReturn(List.of(
                new HomeworkTargetRepository.StudentAssignmentView(assignmentId, "Tarea", "REVIEWED", Instant.now())));

        StudentProfileResponse after = service.getProfile(studentId);
        assertThat(after.progress().homeworkBreakdown().submitted()).isZero();
        assertThat(after.progress().homeworkBreakdown().completed()).isEqualTo(1);
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
