package com.kuky.backend.admin.service;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.units.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentProfileAdminService {

    private static final Set<String> COMPLETED_HOMEWORK_STATUSES = Set.of("REVIEWED", "GRADED");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final HomeworkTargetRepository homeworkTargetRepository;
    private final PresentationRepository presentationRepository;
    private final UnitRepository unitRepository;

    public StudentProfileAdminService(UserRepository userRepository,
                                      BookingRepository bookingRepository,
                                      HomeworkTargetRepository homeworkTargetRepository,
                                      PresentationRepository presentationRepository,
                                      UnitRepository unitRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.homeworkTargetRepository = homeworkTargetRepository;
        this.presentationRepository = presentationRepository;
        this.unitRepository = unitRepository;
    }

    public StudentProfileResponse getProfile(UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));

        List<StudentProfileBookingDto> bookings = bookingRepository.findByUserId(studentId).stream()
                .map(this::toBookingDto)
                .toList();

        List<StudentProfileHomeworkDto> homeworks = homeworkTargetRepository
                .findAssignmentsForStudent(studentId).stream()
                .map(v -> new StudentProfileHomeworkDto(v.assignmentId(), v.title(), v.status(), v.submittedAt(),
                        "MANUAL".equals(v.format()) && "SUBMITTED".equals(v.status()), v.submissionId()))
                .toList();

        List<StudentProfilePresentationDto> presentations = presentationRepository
                .findSharedSummariesForUser(studentId).stream()
                .map(s -> new StudentProfilePresentationDto(s.id(), s.title(), s.level()))
                .toList();

        StudentProgressDto progress = new StudentProgressDto(
                computeUnitProgress(studentId),
                computeHomeworkBreakdown(homeworks),
                computeAttendedClasses(bookings));

        return new StudentProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getAvatarImageId(),
                user.getCreatedAt(),
                bookings,
                homeworks,
                presentations,
                progress);
    }

    private List<UnitProgressDto> computeUnitProgress(UUID studentId) {
        return unitRepository.findProgressForStudent(studentId).stream()
                .map(v -> new UnitProgressDto(
                        v.unitId(), v.subject(), v.level(),
                        v.totalHomeworks(), v.completedHomeworks(),
                        v.totalHomeworks() > 0 && v.completedHomeworks() == v.totalHomeworks()))
                .toList();
    }

    private HomeworkBreakdownDto computeHomeworkBreakdown(List<StudentProfileHomeworkDto> homeworks) {
        int pending = 0;
        int submitted = 0;
        int completed = 0;
        for (StudentProfileHomeworkDto hw : homeworks) {
            if (COMPLETED_HOMEWORK_STATUSES.contains(hw.status())) {
                completed++;
            } else if ("SUBMITTED".equals(hw.status())) {
                submitted++;
            } else {
                pending++;
            }
        }
        return new HomeworkBreakdownDto(pending, submitted, completed);
    }

    private int computeAttendedClasses(List<StudentProfileBookingDto> bookings) {
        Instant now = Instant.now();
        return (int) bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.status()) && b.slotStart().isBefore(now) && !b.noShow())
                .count();
    }

    private StudentProfileBookingDto toBookingDto(Booking b) {
        return new StudentProfileBookingDto(
                b.getId(),
                b.getSlotStart(),
                b.getSlotStart().plusSeconds((long) b.getDurationMinutes() * 60),
                b.getStatus(),
                b.getZoomJoinUrl(),
                b.isNoShow());
    }
}
