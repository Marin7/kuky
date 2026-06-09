package com.kuky.backend.scheduling.controller;

import com.kuky.backend.scheduling.dto.BookingResponse;
import com.kuky.backend.scheduling.dto.CreateBookingRequest;
import com.kuky.backend.scheduling.dto.MyBookingsResponse;
import com.kuky.backend.scheduling.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(email, request.slotStart());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<MyBookingsResponse> listBookings(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(bookingService.listForUser(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id) {
        bookingService.cancelBooking(email, id);
        return ResponseEntity.noContent().build();
    }
}
