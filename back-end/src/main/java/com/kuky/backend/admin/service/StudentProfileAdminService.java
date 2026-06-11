package com.kuky.backend.admin.service;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StudentProfileAdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final HomeworkTargetRepository homeworkTargetRepository;
    private final PresentationRepository presentationRepository;

    public StudentProfileAdminService(UserRepository userRepository,
                                      BookingRepository bookingRepository,
                                      HomeworkTargetRepository homeworkTargetRepository,
                                      PresentationRepository presentationRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.homeworkTargetRepository = homeworkTargetRepository;
        this.presentationRepository = presentationRepository;
    }

    public StudentProfileResponse getProfile(UUID studentId) {
        User user = userRepository.findById(studentId)
                .filter(u -> "STUDENT".equals(u.getRole()))
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));

        List<StudentProfileBookingDto> bookings = bookingRepository.findByUserId(studentId).stream()
                .map(this::toBookingDto)
                .toList();

        List<StudentProfileHomeworkDto> homeworks = homeworkTargetRepository
                .findAssignmentsForStudent(studentId).stream()
                .map(v -> new StudentProfileHomeworkDto(v.assignmentId(), v.title(), v.status(), v.submittedAt()))
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
                bookings,
                homeworks,
                presentations);
    }

    private StudentProfileBookingDto toBookingDto(Booking b) {
        return new StudentProfileBookingDto(
                b.getId(),
                b.getSlotStart(),
                b.getSlotStart().plusSeconds((long) b.getDurationMinutes() * 60),
                b.getStatus(),
                b.getZoomJoinUrl());
    }
}
