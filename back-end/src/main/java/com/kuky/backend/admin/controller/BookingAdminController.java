package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.AdminBookingDto;
import com.kuky.backend.admin.dto.SetNoShowRequest;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public BookingAdminController(BookingRepository bookingRepository,
                                  BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<AdminBookingDto> getUpcomingBookings() {
        return bookingRepository.findUpcomingBookingsForAdmin(Instant.now()).stream()
                .map(v -> new AdminBookingDto(v.id(), v.studentId(), v.email(),
                        v.firstName(), v.lastName(), v.username(),
                        v.slotStart(), v.slotEnd(), v.zoomJoinUrl()))
                .toList();
    }

    /** Teacher cancels a class — bypasses the 24h student cutoff. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID id) {
        bookingService.cancelBookingAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    /** Teacher marks (or unmarks) a past confirmed class as a no-show. */
    @PutMapping("/{id}/no-show")
    public ResponseEntity<Void> setNoShow(@PathVariable UUID id, @RequestBody SetNoShowRequest request) {
        bookingService.setNoShow(id, request.noShow());
        return ResponseEntity.noContent().build();
    }
}
