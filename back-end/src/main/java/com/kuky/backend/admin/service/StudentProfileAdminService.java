package com.kuky.backend.admin.service;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.InterestCatalogue;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.HomeworkDueDates;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class StudentProfileAdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final HomeworkTargetRepository homeworkTargetRepository;
    private final PresentationRepository presentationRepository;
    private final SchedulingProperties schedulingProperties;

    public StudentProfileAdminService(UserRepository userRepository,
                                      BookingRepository bookingRepository,
                                      HomeworkTargetRepository homeworkTargetRepository,
                                      PresentationRepository presentationRepository,
                                      SchedulingProperties schedulingProperties) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.homeworkTargetRepository = homeworkTargetRepository;
        this.presentationRepository = presentationRepository;
        this.schedulingProperties = schedulingProperties;
    }

    public StudentProfileResponse getProfile(UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));

        List<StudentProfileBookingDto> bookings = bookingRepository.findByUserId(studentId).stream()
                .map(b -> toBookingDto(b, studentId))
                .toList();

        LocalDate today = LocalDate.now(ZoneId.of(schedulingProperties.getScheduling().getTeacherTimezone()));
        List<StudentProfileHomeworkDto> homeworks = homeworkTargetRepository
                .findAssignmentsForStudent(studentId).stream()
                .map(v -> new StudentProfileHomeworkDto(v.assignmentId(), v.title(), v.status(), v.submittedAt(),
                        "MANUAL".equals(v.format()) && "SUBMITTED".equals(v.status()), v.submissionId(),
                        v.scorePercent(), v.hasTeacherFeedback(), v.unseen(), v.format(), v.dueOn(),
                        HomeworkDueDates.overdue(v.dueOn(), today, v.status())))
                .toList();

        List<StudentProfilePresentationDto> presentations = presentationRepository
                .findSharedSummariesForUser(studentId).stream()
                .map(s -> new StudentProfilePresentationDto(s.id(), s.title(), s.level()))
                .toList();

        return new StudentProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getAvatarImageId(),
                user.getCreatedAt(),
                InterestCatalogue.filterKnown(userRepository.findInterestCodesByUserId(studentId)),
                user.getInterestsNote(),
                bookings,
                homeworks,
                presentations);
    }

    /**
     * {@code viewedStudentId} may be either the booking student or the companion student on a
     * shared booking (findByUserId matches both) — the no-show flag and role reported here MUST
     * reflect whichever one this profile actually belongs to, not always the booking student's.
     */
    private StudentProfileBookingDto toBookingDto(Booking b, UUID viewedStudentId) {
        boolean isCompanionStudent = viewedStudentId.equals(b.getCompanionStudentId());
        boolean noShow = isCompanionStudent ? Boolean.TRUE.equals(b.getCompanionStudentNoShow()) : b.isNoShow();
        return new StudentProfileBookingDto(
                b.getId(),
                b.getSlotStart(),
                b.getSlotStart().plusSeconds((long) b.getDurationMinutes() * 60),
                b.getStatus(),
                b.getZoomJoinUrl(),
                noShow,
                isCompanionStudent);
    }
}
