package com.kuky.backend.scheduling;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.exception.BookingNotFoundException;
import com.kuky.backend.scheduling.exception.SlotUnavailableException;
import com.kuky.backend.scheduling.meeting.MeetingProvider;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import com.kuky.backend.scheduling.service.BookingEmailService;
import com.kuky.backend.scheduling.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private AvailabilityService availabilityService;
    private MeetingProvider meetingProvider;
    private BookingEmailService emailService;
    private BookingService service;

    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);
        availabilityService = mock(AvailabilityService.class);
        meetingProvider = mock(MeetingProvider.class);
        emailService = mock(BookingEmailService.class);
        SchedulingProperties props = new SchedulingProperties(); // real defaults: 60/90 min, Madrid, etc.
        service = new BookingService(bookingRepository, userRepository, availabilityService,
                meetingProvider, emailService, props);
    }

    private Booking booking(String status, Instant slotStart) {
        Booking b = new Booking();
        b.setId(bookingId);
        b.setUserId(UUID.randomUUID());
        b.setStatus(status);
        b.setSlotStart(slotStart);
        b.setDurationMinutes(50);
        return b;
    }

    private User user(String email, boolean extendedClassEligible) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setExtendedClassEligible(extendedClassEligible);
        return u;
    }

    @Test
    void setNoShowSucceedsForPastConfirmedBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS))));

        service.setNoShow(bookingId, true);

        verify(bookingRepository).setNoShow(bookingId, true);
    }

    @Test
    void setNoShowThrowsWhenBookingNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setNoShow(bookingId, true))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void setNoShowThrowsForFutureBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.setNoShow(bookingId, true))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW));
    }

    @Test
    void setNoShowThrowsForCancelledBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CANCELLED", Instant.now().minus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.setNoShow(bookingId, false))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW));
    }

    // --- createBooking: duration validation, eligibility, and pass-through (US2) -------------

    @Test
    void createBooking_90Minutes_succeedsForEligibleUser_andPassesDurationThrough() {
        String email = "eligible@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));
        when(bookingRepository.insert(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(bookingId);
            return b;
        });
        when(meetingProvider.create(eq(slotStart), eq(90), anyString()))
                .thenReturn(new MeetingProvider.MeetingDetails("zoom-1", "https://zoom.example/1"));

        var response = service.createBooking(email, slotStart, 90);

        assertThat(response.durationMinutes()).isEqualTo(90);
        verify(availabilityService).validateBookable(slotStart, 90);
        verify(meetingProvider).create(slotStart, 90, "Clase de español — " + email);
        verify(emailService).sendConfirmation(eq(email), anyString(), any(), eq(slotStart), eq(90), anyString());
    }

    @Test
    void createBooking_90Minutes_throwsNotEligible_forUserWithoutExtendedClassEligibility() {
        String email = "ineligible@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, false)));

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 90))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_EXTENDED));

        verify(bookingRepository, never()).insert(any());
    }

    @Test
    void createBooking_invalidDuration_throwsInvalidDuration() {
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 45))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.INVALID_DURATION));

        verify(bookingRepository, never()).insert(any());
    }

    @Test
    void createBooking_60Minutes_doesNotRequireEligibility() {
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, false)));
        when(bookingRepository.insert(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(bookingId);
            return b;
        });
        when(meetingProvider.create(eq(slotStart), eq(60), anyString()))
                .thenReturn(new MeetingProvider.MeetingDetails("zoom-2", "https://zoom.example/2"));

        var response = service.createBooking(email, slotStart, 60);

        assertThat(response.durationMinutes()).isEqualTo(60);
    }

    @Test
    void createBooking_overlapExclusionViolation_isTranslatedToSlotUnavailable() {
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));
        when(bookingRepository.insert(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("exclusion constraint violated"));

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 90))
                .isInstanceOf(SlotUnavailableException.class);
    }
}
