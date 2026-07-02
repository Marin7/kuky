package com.kuky.backend.scheduling;

import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.exception.BookingNotFoundException;
import com.kuky.backend.scheduling.meeting.MeetingProvider;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import com.kuky.backend.scheduling.service.BookingEmailService;
import com.kuky.backend.scheduling.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private BookingService service;

    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AvailabilityService availabilityService = mock(AvailabilityService.class);
        MeetingProvider meetingProvider = mock(MeetingProvider.class);
        BookingEmailService emailService = mock(BookingEmailService.class);
        SchedulingProperties props = mock(SchedulingProperties.class);
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
}
