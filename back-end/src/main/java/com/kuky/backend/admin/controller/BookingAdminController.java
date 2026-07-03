package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.AdminBookingDto;
import com.kuky.backend.admin.dto.AttachCompanionStudentRequest;
import com.kuky.backend.admin.dto.SetNoShowRequest;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin-only view of upcoming confirmed bookings, plus teacher-initiated cancellation.
 * Secured by the /api/v1/admin/** matcher in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/bookings")
public class BookingAdminController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingAdminController(BookingRepository bookingRepository,
                                  BookingService bookingService,
                                  UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AdminBookingDto> getUpcomingBookings() {
        return bookingRepository.findUpcomingBookingsForAdmin(Instant.now()).stream()
                .map(v -> new AdminBookingDto(v.id(), v.studentId(), v.email(),
                        v.firstName(), v.lastName(), v.username(),
                        v.slotStart(), v.slotEnd(), v.zoomJoinUrl(),
                        v.companionStudentId(), v.companionStudentEmail(),
                        v.companionStudentFirstName(), v.companionStudentLastName(),
                        v.companionStudentUsername(), v.companionStudentNoShow()))
                .toList();
    }

    /** Teacher cancels a class — bypasses the 24h student cutoff. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID id) {
        bookingService.cancelBookingAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    /** Teacher marks (or unmarks) a past confirmed class as a no-show, for either student. */
    @PutMapping("/{id}/no-show")
    public ResponseEntity<Void> setNoShow(@PathVariable UUID id, @RequestBody SetNoShowRequest request) {
        bookingService.setNoShow(id, request.noShow(), request.studentRoleOrDefault());
        return ResponseEntity.noContent().build();
    }

    /** Teacher attaches a companion student to an existing, upcoming, confirmed booking — both students share the class with equal standing. */
    @PostMapping("/{id}/companion-student")
    public AdminBookingDto attachCompanionStudent(@PathVariable UUID id, @Valid @RequestBody AttachCompanionStudentRequest request) {
        Booking booking = bookingService.attachCompanionStudent(id, request.studentId());
        return toAdminBookingDto(booking);
    }

    /** Teacher removes the companion student from a booking, without cancelling the class. */
    @DeleteMapping("/{id}/companion-student")
    public ResponseEntity<Void> detachCompanionStudent(@PathVariable UUID id) {
        bookingService.detachCompanionStudent(id);
        return ResponseEntity.noContent().build();
    }

    private AdminBookingDto toAdminBookingDto(Booking booking) {
        User bookingStudent = userRepository.findById(booking.getUserId()).orElseThrow();
        User companion = booking.getCompanionStudentId() == null
                ? null
                : userRepository.findById(booking.getCompanionStudentId()).orElse(null);
        return new AdminBookingDto(
                booking.getId(), bookingStudent.getId(), bookingStudent.getEmail(),
                bookingStudent.getFirstName(), bookingStudent.getLastName(), bookingStudent.getUsername(),
                booking.getSlotStart(), booking.getSlotEnd(), booking.getZoomJoinUrl(),
                companion == null ? null : companion.getId(),
                companion == null ? null : companion.getEmail(),
                companion == null ? null : companion.getFirstName(),
                companion == null ? null : companion.getLastName(),
                companion == null ? null : companion.getUsername(),
                booking.getCompanionStudentNoShow());
    }
}
