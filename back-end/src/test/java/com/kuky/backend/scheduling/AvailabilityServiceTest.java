package com.kuky.backend.scheduling;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.DayWindow;
import com.kuky.backend.scheduling.model.Slot;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pure-logic tests for slot derivation and week materialization — no Spring context, no database. */
class AvailabilityServiceTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    // Monday 2026-06-15, 08:00 Madrid (06:00 UTC in summer/CEST). Horizon: 06-15 .. 06-29.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T06:00:00Z"), MADRID);
    private static final LocalDate WEEK_1 = LocalDate.of(2026, 6, 15);
    private static final LocalDate WEEK_2 = LocalDate.of(2026, 6, 22);
    private static final LocalDate WEEK_3 = LocalDate.of(2026, 6, 29);
    private static final LocalDate WEEK_4 = LocalDate.of(2026, 7, 6);

    private AvailabilityRepository availabilityRepo;
    private BookingRepository bookingRepo;
    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        availabilityRepo = mock(AvailabilityRepository.class);
        bookingRepo = mock(BookingRepository.class);
        when(bookingRepo.findConfirmedSlotStartsBetween(any(), any())).thenReturn(List.of());
        // Default: all horizon weeks already materialized, so ensureWeeksMaterialized is a no-op.
        when(availabilityRepo.findMaterializedWeekStarts(any(), any()))
                .thenReturn(List.of(WEEK_1, WEEK_2, WEEK_3, WEEK_4));
        when(availabilityRepo.findDayWindowsBetween(any(), any())).thenReturn(List.of());
        when(availabilityRepo.findDayWindows(any())).thenReturn(List.of());
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

    // --- slot derivation from the materialized snapshot --------------------------------------

    @Test
    void generatesHourlySlotsInsideAWindow() {
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        assertThat(openTimesOn(LocalDate.of(2026, 6, 24)))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void splitWindowsLeaveALunchGap() {
        when(availabilityRepo.findDayWindowsBetween(any(), any())).thenReturn(List.of(
                dw(LocalDate.of(2026, 6, 24), "09:00", "12:00"),
                dw(LocalDate.of(2026, 6, 24), "14:00", "18:00")));

        List<LocalTime> times = openTimesOn(LocalDate.of(2026, 6, 24));
        assertThat(times).contains(LocalTime.of(11, 0), LocalTime.of(14, 0));
        assertThat(times).doesNotContain(LocalTime.of(12, 0), LocalTime.of(13, 0));
    }

    @Test
    void dateWithNoWindowsHasNoSlots() {
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        // Saturday 2026-06-20 has no window in the snapshot.
        assertThat(openCountOn(LocalDate.of(2026, 6, 20))).isZero();
    }

    @Test
    void aWindowOnOneDateDoesNotLeakToAdjacentDates() {
        // US3 isolation: a customization on 06-24 must not appear on 06-23.
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        assertThat(openCountOn(LocalDate.of(2026, 6, 23))).isZero();
        assertThat(openCountOn(LocalDate.of(2026, 6, 24))).isEqualTo(3);
    }

    @Test
    void slotsWithinTheLeadWindowAreUnavailable() {
        // Monday (today) 09:00 is < 24h ahead of the 08:00 "now" → not OPEN.
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(WEEK_1, "09:00", "12:00")));

        assertThat(openCountOn(WEEK_1)).isZero();
    }

    @Test
    void findsConfirmedBookingsOutsideSavedAvailability() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "12:00")));

        Instant insideStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        Instant outsideStart = date.atTime(16, 0).atZone(MADRID).toInstant(); // no window
        when(bookingRepo.findUpcomingConfirmedBookings(any())).thenReturn(List.of(
                new BookingRepository.ConfirmedBookingView(UUID.randomUUID(), "in@x.com", insideStart),
                new BookingRepository.ConfirmedBookingView(UUID.randomUUID(), "out@x.com", outsideStart)));

        var conflicts = service.findConfirmedBookingsOutsideAvailability();
        assertThat(conflicts).extracting(BookingRepository.ConfirmedBookingView::email)
                .containsExactly("out@x.com");
    }

    // --- materialization (snapshot per week) -------------------------------------------------

    @Test
    void doesNotReMaterializeAlreadySnapshottedWeeks() {
        // Both weeks materialized (default stub) → no write.
        service.ensureCurrentHorizonMaterialized();
        verify(availabilityRepo, never()).materializeWeek(any(), any());
    }

    @Test
    void materializesMissingWeeksFromTheTemplate() {
        when(availabilityRepo.findMaterializedWeekStarts(any(), any())).thenReturn(List.of());
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(3, "09:00", "12:00"))); // Wednesdays

        service.ensureCurrentHorizonMaterialized();

        ArgumentCaptor<List<DayWindow>> windows = captor();
        verify(availabilityRepo).materializeWeek(eq(WEEK_1), windows.capture());
        verify(availabilityRepo).materializeWeek(eq(WEEK_2), any());
        // Week 1's Wednesday is 2026-06-17 → window copied from the template.
        assertThat(windows.getValue())
                .containsExactly(dw(LocalDate.of(2026, 6, 17), "09:00", "12:00"));
    }

    @Test
    void emptyTemplateMaterializesFullyUnavailableWeeks() {
        when(availabilityRepo.findMaterializedWeekStarts(any(), any())).thenReturn(List.of());
        when(availabilityRepo.findAllRules()).thenReturn(List.of());

        service.ensureCurrentHorizonMaterialized();

        verify(availabilityRepo).materializeWeek(eq(WEEK_1), eq(List.of()));
        verify(availabilityRepo).materializeWeek(eq(WEEK_2), eq(List.of()));
    }

    @Test
    void materializesOnlyWeeksNotYetSnapshotted() {
        // US4 / FR-007: week 1 already materialized, week 2 not. A template change must only seed
        // the not-yet-materialized week, never rewrite the already-materialized one.
        when(availabilityRepo.findMaterializedWeekStarts(any(), any())).thenReturn(List.of(WEEK_1));
        when(availabilityRepo.findAllRules()).thenReturn(List.of(rule(1, "10:00", "11:00"))); // new template

        service.ensureCurrentHorizonMaterialized();

        verify(availabilityRepo, never()).materializeWeek(eq(WEEK_1), any());
        verify(availabilityRepo).materializeWeek(eq(WEEK_2), any());
    }

    private static DayWindow dw(LocalDate date, String start, String end) {
        return new DayWindow(date, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static AvailabilityRule rule(int dow, String start, String end) {
        return new AvailabilityRule(dow, LocalTime.parse(start), LocalTime.parse(end));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<DayWindow>> captor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
