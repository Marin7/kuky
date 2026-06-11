package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.AdminBookingDto;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Admin-only view of upcoming confirmed bookings.
 * Secured by the /api/v1/admin/** matcher in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/bookings")
public class BookingAdminController {

    private final BookingRepository bookingRepository;

    public BookingAdminController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public List<AdminBookingDto> getUpcomingBookings() {
        return bookingRepository.findUpcomingBookingsForAdmin(Instant.now()).stream()
                .map(v -> new AdminBookingDto(v.id(), v.studentId(), v.email(),
                        v.firstName(), v.lastName(), v.username(),
                        v.slotStart(), v.slotEnd(), v.zoomJoinUrl()))
                .toList();
    }
}
