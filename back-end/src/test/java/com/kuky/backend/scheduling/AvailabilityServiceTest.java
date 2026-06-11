package com.kuky.backend.scheduling;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.model.AvailabilityException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.Slot;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pure-logic tests for slot derivation — no Spring context, no database. */
class AvailabilityServiceTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    // Monday 2026-06-15, 08:00 Madrid (06:00 UTC in summer/CEST).
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T06:00:00Z"), MADRID);

    private AvailabilityRepository availabilityRepo;
    private BookingRepository bookingRepo;
    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        availabilityRepo = mock(AvailabilityRepository.class);
        bookingRepo = mock(BookingRepository.class);
        when(bookingRepo.findConfirmedSlotStartsBetween(any(), any())).thenReturn(List.of());
        SchedulingProperties props = new SchedulingProperties(); // defaults: Madrid, 60min, 24h lead
        service = new AvailabilityService(props, bookingRepo, availabilityRepo, CLOCK);
    }

    private List<LocalTime> openTimesOn(LocalDate date) {
        return service.generateSchedule().stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .filter(s -> s.getStatus() == Slot.Status.OPEN)
                .map(s -> s.getStart().atZone(MADRID).toLocalTime())
                .toList();
    }

    private long openCountOn(LocalDate date) {
        return service.generateSchedule().stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .filter(s -> s.getStatus() == Slot.Status.OPEN)
                .count();
    }

    @Test
    void generatesHourlySlotsInsideAWeeklyWindow() {
        // Wednesday 2026-06-24 is 9 days out (well past the 24h lead window).
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(3, "09:00", "12:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of());

        assertThat(openTimesOn(LocalDate.of(2026, 6, 24)))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void splitWindowsLeaveALunchGap() {
        when(availabilityRepo.findAllRules()).thenReturn(List.of(
                rule(3, "09:00", "12:00"), rule(3, "14:00", "18:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of());

        List<LocalTime> times = openTimesOn(LocalDate.of(2026, 6, 24));
        assertThat(times).contains(LocalTime.of(11, 0), LocalTime.of(14, 0));
        assertThat(times).doesNotContain(LocalTime.of(12, 0), LocalTime.of(13, 0));
    }

    @Test
    void weekendWithNoRulesHasNoSlots() {
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(3, "09:00", "12:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of());

        // Saturday 2026-06-20 — no rule for day 6.
        assertThat(openCountOn(LocalDate.of(2026, 6, 20))).isZero();
    }

    @Test
    void blockExceptionRemovesTime() {
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(3, "09:00", "12:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of(
                exception(LocalDate.of(2026, 6, 24), AvailabilityException.Kind.BLOCK, "10:00", "11:00")));

        assertThat(openTimesOn(LocalDate.of(2026, 6, 24)))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    @Test
    void openExceptionAddsTimeOnANonRuleDay() {
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(3, "09:00", "12:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of(
                exception(LocalDate.of(2026, 6, 20), AvailabilityException.Kind.OPEN, "10:00", "12:00")));

        // Saturday gains slots purely from the OPEN exception.
        assertThat(openTimesOn(LocalDate.of(2026, 6, 20)))
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void slotsWithinTheLeadWindowAreUnavailable() {
        // Monday (today) 09:00 is < 24h ahead of the 08:00 "now" → not OPEN.
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(1, "09:00", "12:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of());

        assertThat(openCountOn(LocalDate.of(2026, 6, 15))).isZero();
    }

    @Test
    void findsConfirmedBookingsOutsideSavedAvailability() {
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(3, "09:00", "12:00")));
        when(availabilityRepo.findExceptionsBetween(any(), any())).thenReturn(List.of());

        Instant insideStart = LocalDate.of(2026, 6, 24).atTime(10, 0).atZone(MADRID).toInstant();
        Instant outsideStart = LocalDate.of(2026, 6, 24).atTime(16, 0).atZone(MADRID).toInstant(); // no window
        when(bookingRepo.findUpcomingConfirmedBookings(any())).thenReturn(List.of(
                new BookingRepository.ConfirmedBookingView(UUID.randomUUID(), "in@x.com", insideStart),
                new BookingRepository.ConfirmedBookingView(UUID.randomUUID(), "out@x.com", outsideStart)));

        var conflicts = service.findConfirmedBookingsOutsideAvailability();
        assertThat(conflicts).extracting(BookingRepository.ConfirmedBookingView::email)
                .containsExactly("out@x.com");
    }

    private static AvailabilityRule rule(int dow, String start, String end) {
        return new AvailabilityRule(dow, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static AvailabilityException exception(LocalDate date, AvailabilityException.Kind kind,
                                                   String start, String end) {
        AvailabilityException e = new AvailabilityException();
        e.setDate(date);
        e.setKind(kind);
        e.setStartTime(LocalTime.parse(start));
        e.setEndTime(LocalTime.parse(end));
        return e;
    }
}
