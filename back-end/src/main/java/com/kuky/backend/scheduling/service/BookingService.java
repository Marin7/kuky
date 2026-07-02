package com.kuky.backend.scheduling.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.dto.BookingResponse;
import com.kuky.backend.scheduling.dto.BookingSummary;
import com.kuky.backend.scheduling.dto.MyBookingsResponse;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.exception.BookingNotFoundException;
import com.kuky.backend.scheduling.exception.MeetingProvisioningException;
import com.kuky.backend.scheduling.exception.SlotUnavailableException;
import com.kuky.backend.scheduling.meeting.MeetingProvider;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final MeetingProvider meetingProvider;
    private final BookingEmailService emailService;
    private final SchedulingProperties props;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          AvailabilityService availabilityService,
                          MeetingProvider meetingProvider,
                          BookingEmailService emailService,
                          SchedulingProperties props) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
        this.meetingProvider = meetingProvider;
        this.emailService = emailService;
        this.props = props;
    }

    public BookingResponse createBooking(String userEmail, Instant slotStart) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        // Validate slot is bookable (range + lead time)
        availabilityService.validateBookable(slotStart);

        int duration = props.getScheduling().getClassDurationMinutes();

        // Reserve the slot
        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setSlotStart(slotStart);
        booking.setDurationMinutes(duration);
        booking.setStatus("CONFIRMED");

        try {
            booking = bookingRepository.insert(booking);
        } catch (DuplicateKeyException e) {
            throw new SlotUnavailableException("Esta hora ya ha sido reservada.");
        }

        // Provision Zoom meeting — compensate on failure
        MeetingProvider.MeetingDetails meeting;
        try {
            String topic = "Clase de español — " + userEmail;
            meeting = meetingProvider.create(slotStart, duration, topic);
        } catch (MeetingProvisioningException e) {
            bookingRepository.delete(booking.getId());
            throw e;
        }

        bookingRepository.updateZoomDetails(booking.getId(), meeting.meetingId(), meeting.joinUrl());
        booking.setZoomMeetingId(meeting.meetingId());
        booking.setZoomJoinUrl(meeting.joinUrl());

        emailService.sendConfirmation(userEmail,
                props.getScheduling().getTeacherEmail(),
                slotStart, duration, meeting.joinUrl());

        return toResponse(booking);
    }

    public MyBookingsResponse listForUser(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        List<Booking> all = bookingRepository.findByUserId(user.getId());
        Instant now = Instant.now();
        int cancelCutoff = props.getScheduling().getCancelCutoffHours();

        List<BookingSummary> upcoming = all.stream()
                .filter(b -> b.getSlotEnd().isAfter(now) && "CONFIRMED".equals(b.getStatus()))
                .map(b -> toSummary(b, cancelCutoff))
                .toList();

        List<BookingSummary> past = all.stream()
                .filter(b -> !b.getSlotEnd().isAfter(now) || "CANCELLED".equals(b.getStatus()))
                .map(b -> toSummary(b, cancelCutoff))
                .toList();

        return new MyBookingsResponse(upcoming, past);
    }

    public void cancelBooking(String userEmail, UUID bookingId) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getUserId().equals(user.getId()))
                .orElseThrow(() -> new BookingNotFoundException("Reserva no encontrada."));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.STATE);
        }

        int cutoff = props.getScheduling().getCancelCutoffHours();
        Instant cutoffInstant = Instant.now().plusSeconds((long) cutoff * 3600);
        if (!booking.getSlotStart().isAfter(cutoffInstant)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.CUTOFF);
        }

        bookingRepository.markCancelled(bookingId, Instant.now());

        if (booking.getZoomMeetingId() != null) {
            meetingProvider.cancel(booking.getZoomMeetingId());
        }

        emailService.sendCancellation(
                props.getScheduling().getTeacherEmail(),
                userEmail,
                booking.getSlotStart());
    }

    /**
     * Teacher-initiated cancellation. Unlike {@link #cancelBooking}, this bypasses the
     * owner check and the 24h cancellation cutoff — the teacher may cancel any confirmed
     * class at any time. Secured by the /api/v1/admin/** matcher in SecurityConfig.
     */
    public void cancelBookingAsAdmin(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Reserva no encontrada."));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.STATE);
        }

        bookingRepository.markCancelled(bookingId, Instant.now());

        if (booking.getZoomMeetingId() != null) {
            meetingProvider.cancel(booking.getZoomMeetingId());
        }

        userRepository.findById(booking.getUserId()).ifPresent(student ->
                emailService.sendCancellationByTeacher(
                        student.getEmail(),
                        props.getScheduling().getTeacherEmail(),
                        booking.getSlotStart()));
    }

    /**
     * Teacher-initiated attendance correction — only meaningful for a class that already
     * happened. Secured by the /api/v1/admin/** matcher in SecurityConfig.
     */
    public void setNoShow(UUID bookingId, boolean noShow) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Reserva no encontrada."));

        boolean isPastConfirmed = "CONFIRMED".equals(booking.getStatus())
                && booking.getSlotStart().isBefore(Instant.now());
        if (!isPastConfirmed) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW);
        }

        bookingRepository.setNoShow(bookingId, noShow);
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getSlotStart(),
                b.getSlotEnd(),
                b.getDurationMinutes(),
                b.getStatus(),
                b.getZoomJoinUrl(),
                b.getCreatedAt()
        );
    }

    private BookingSummary toSummary(Booking b, int cancelCutoffHours) {
        Instant cutoffInstant = Instant.now().plusSeconds((long) cancelCutoffHours * 3600);
        boolean cancellable = "CONFIRMED".equals(b.getStatus())
                && b.getSlotStart().isAfter(cutoffInstant);
        return new BookingSummary(
                b.getId(),
                b.getSlotStart(),
                b.getSlotEnd(),
                b.getStatus(),
                b.getZoomJoinUrl(),
                cancellable
        );
    }
}
