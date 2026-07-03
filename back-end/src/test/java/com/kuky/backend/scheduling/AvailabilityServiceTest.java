package com.kuky.backend.scheduling;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.exception.SlotUnavailableException;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any())).thenReturn(List.of());
        // Default: all horizon weeks already materialized, so ensureWeeksMaterialized is a no-op.
        when(availabilityRepo.findMaterializedWeekStarts(any(), any()))
                .thenReturn(List.of(WEEK_1, WEEK_2, WEEK_3, WEEK_4));
        when(availabilityRepo.findDayWindowsBetween(any(), any())).thenReturn(List.of());
        when(availabilityRepo.findDayWindows(any())).thenReturn(List.of());
        SchedulingProperties props = new SchedulingProperties(); // defaults: Madrid, 60/90min, 24h lead
        service = new AvailabilityService(props, bookingRepo, availabilityRepo, CLOCK);
    }

    private List<LocalTime> openTimesOn(LocalDate date, int durationMinutes) {
        return service.generateSchedule(durationMinutes).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .filter(s -> s.getStatus() == Slot.Status.OPEN)
                .map(s -> s.getStart().atZone(MADRID).toLocalTime())
                .toList();
    }

    private static Slot.Status statusAt(List<Slot> slots, LocalTime time) {
        return slots.stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalTime().equals(time))
                .findFirst().orElseThrow().getStatus();
    }

    private long openCountOn(LocalDate date, int durationMinutes) {
        return service.generateSchedule(durationMinutes).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .filter(s -> s.getStatus() == Slot.Status.OPEN)
                .count();
    }

    // --- slot derivation from the materialized snapshot --------------------------------------

    @Test
    void generatesHourlySlotsInsideAWindow() {
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        assertThat(openTimesOn(LocalDate.of(2026, 6, 24), 60))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void splitWindowsLeaveALunchGap() {
        when(availabilityRepo.findDayWindowsBetween(any(), any())).thenReturn(List.of(
                dw(LocalDate.of(2026, 6, 24), "09:00", "12:00"),
                dw(LocalDate.of(2026, 6, 24), "14:00", "18:00")));

        List<LocalTime> times = openTimesOn(LocalDate.of(2026, 6, 24), 60);
        assertThat(times).contains(LocalTime.of(11, 0), LocalTime.of(14, 0));
        assertThat(times).doesNotContain(LocalTime.of(12, 0), LocalTime.of(13, 0));
    }

    @Test
    void dateWithNoWindowsHasNoSlots() {
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        // Saturday 2026-06-20 has no window in the snapshot.
        assertThat(openCountOn(LocalDate.of(2026, 6, 20), 60)).isZero();
    }

    @Test
    void aWindowOnOneDateDoesNotLeakToAdjacentDates() {
        // US3 isolation: a customization on 06-24 must not appear on 06-23.
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        assertThat(openCountOn(LocalDate.of(2026, 6, 23), 60)).isZero();
        assertThat(openCountOn(LocalDate.of(2026, 6, 24), 60)).isEqualTo(3);
    }

    @Test
    void slotsWithinTheLeadWindowAreUnavailable() {
        // Monday (today) 09:00 is < 24h ahead of the 08:00 "now" → not OPEN.
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(WEEK_1, "09:00", "12:00")));

        assertThat(openCountOn(WEEK_1, 60)).isZero();
    }

    @Test
    void findsConfirmedBookingsOutsideSavedAvailability() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "12:00")));

        Instant insideStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        Instant outsideStart = date.atTime(16, 0).atZone(MADRID).toInstant(); // no window
        when(bookingRepo.findUpcomingConfirmedBookings(any())).thenReturn(List.of(
                new BookingRepository.ConfirmedBookingView(UUID.randomUUID(), "in@x.com", insideStart, 60),
                new BookingRepository.ConfirmedBookingView(UUID.randomUUID(), "out@x.com", outsideStart, 60)));

        var conflicts = service.findConfirmedBookingsOutsideAvailability();
        assertThat(conflicts).extracting(BookingRepository.ConfirmedBookingView::email)
                .containsExactly("out@x.com");
    }

    // --- extended (90-minute) duration: slot derivation & overlap (US2) ----------------------

    @Test
    void generatesNinetyMinuteSlotsInsideAWindow() {
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "13:00")));

        // A 4-hour window chunked in 90-minute steps from window start: 09:00, 10:30, 12:00 (12:00+90=13:30 > 13:00 excluded).
        assertThat(openTimesOn(LocalDate.of(2026, 6, 24), 90))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 30));
    }

    @Test
    void sixtyMinuteScheduleIsUnaffectedByNinetyMinuteRequests() {
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "12:00")));

        assertThat(openTimesOn(LocalDate.of(2026, 6, 24), 60))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void ninetyMinuteGenerateScheduleMarksOverlappingSlotBooked_regardlessOfExistingBookingDuration() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));

        // An existing 60-minute booking at 09:30 (09:30-10:30) overlaps the 90-minute candidate
        // slot starting at 09:00 (09:00-10:30) and the one at 10:30 would not overlap it.
        Instant existingStart = date.atTime(9, 30).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 60)));

        List<Slot> slots = service.generateSchedule(90).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .toList();

        Slot nineOClock = slots.stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalTime().equals(LocalTime.of(9, 0)))
                .findFirst().orElseThrow();
        assertThat(nineOClock.getStatus()).isEqualTo(Slot.Status.BOOKED);
    }

    @Test
    void cancelledBookingsDoNotBlockTheSchedule_FR008() {
        // Repository already filters WHERE status = 'CONFIRMED'; a cancelled booking simply never
        // appears in findConfirmedBookingIntervalsBetween, so both grids reopen it automatically.
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(LocalDate.of(2026, 6, 24), "09:00", "13:00")));
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any())).thenReturn(List.of());

        assertThat(openTimesOn(LocalDate.of(2026, 6, 24), 60)).contains(LocalTime.of(9, 0));
        assertThat(openTimesOn(LocalDate.of(2026, 6, 24), 90)).contains(LocalTime.of(9, 0));
    }

    // --- validateBookable: alignment, availability fit, overlap (US2) ------------------------

    @Test
    void validateBookable_60_stillPasses_whenSlotFitsAndIsFree() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "12:00")));
        Instant slotStart = date.atTime(9, 0).atZone(MADRID).toInstant();

        assertThatCode(() -> service.validateBookable(slotStart, 60)).doesNotThrowAnyException();
    }

    @Test
    void validateBookable_90_rejectsInsufficientContiguousAvailability() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        // Only a 60-minute window — a 90-minute booking can never fit.
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "10:00")));
        Instant slotStart = date.atTime(9, 0).atZone(MADRID).toInstant();

        assertThatThrownBy(() -> service.validateBookable(slotStart, 90))
                .isInstanceOf(BookingNotAllowedException.class)
                .extracting(e -> ((BookingNotAllowedException) e).getReason())
                .isEqualTo(BookingNotAllowedException.Reason.RANGE);
    }

    @Test
    void validateBookable_90_succeeds_whenNinetyContiguousMinutesAreFree() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "12:00")));
        Instant slotStart = date.atTime(9, 0).atZone(MADRID).toInstant();

        assertThatCode(() -> service.validateBookable(slotStart, 90)).doesNotThrowAnyException();
    }

    @Test
    void validateBookable_rejectsOverlapWithExistingBookingOfADifferentDuration() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 90-minute booking 09:00-10:30. A new 60-minute request at 10:00 (aligned to the
        // hour, so it passes the alignment check) still overlaps it (10:00-11:00 vs 09:00-10:30).
        Instant existingStart = date.atTime(9, 0).atZone(MADRID).toInstant();
        Instant candidateStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 90)));

        assertThatThrownBy(() -> service.validateBookable(candidateStart, 60))
                .isInstanceOf(SlotUnavailableException.class);
    }

    // --- 15-minute booking buffer (spec 020, US1/US3) -----------------------------------------

    @Test
    void generateSchedule_bufferMarksNearAdjacentSlotsUnavailable_butExactOverlapStaysBooked() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 60-minute booking 10:00-11:00.
        Instant existingStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 60)));

        List<Slot> slots = service.generateSchedule(60).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .toList();

        assertThat(statusAt(slots, LocalTime.of(9, 0))).isEqualTo(Slot.Status.UNAVAILABLE); // 0 min gap before
        assertThat(statusAt(slots, LocalTime.of(10, 0))).isEqualTo(Slot.Status.BOOKED); // the booking itself
        assertThat(statusAt(slots, LocalTime.of(11, 0))).isEqualTo(Slot.Status.UNAVAILABLE); // 0 min gap after
        assertThat(statusAt(slots, LocalTime.of(12, 0))).isEqualTo(Slot.Status.OPEN); // 60 min gap after
    }

    @Test
    void generateSchedule_slotExactlyFifteenMinutesAfterBookingEndsIsOpen() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 45-minute booking 10:00-10:45 — the next hourly slot (11:00) starts exactly 15 min later.
        Instant existingStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 45)));

        List<Slot> slots = service.generateSchedule(60).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .toList();

        assertThat(statusAt(slots, LocalTime.of(11, 0))).isEqualTo(Slot.Status.OPEN);
    }

    @Test
    void generateSchedule_slotFourteenMinutesAfterBookingEndsIsUnavailable() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 46-minute booking 10:00-10:46 — the next hourly slot (11:00) starts only 14 min later.
        Instant existingStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 46)));

        List<Slot> slots = service.generateSchedule(60).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(date))
                .toList();

        assertThat(statusAt(slots, LocalTime.of(11, 0))).isEqualTo(Slot.Status.UNAVAILABLE);
    }

    @Test
    void cancellingABookingReopensItsFormerBufferZone_FR006() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindowsBetween(any(), any()))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        Instant existingStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 60)));

        // Before cancellation: 11:00 sits inside the booking's buffer zone (booking ends 11:00, buffer to 11:15).
        assertThat(openTimesOn(date, 60)).doesNotContain(LocalTime.of(11, 0));

        // Cancellation: findConfirmedBookingIntervalsBetween already filters WHERE status = 'CONFIRMED',
        // so a cancelled booking simply stops being returned — no separate "release" step exists.
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any())).thenReturn(List.of());

        assertThat(openTimesOn(date, 60)).contains(LocalTime.of(11, 0));
    }

    @Test
    void validateBookable_rejectsCandidateStartingWithinBufferAfterExistingBookingEnds() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 50-minute booking 10:00-10:50 — a candidate at 11:00 is only 10 minutes later.
        Instant existingStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 50)));
        Instant candidateStart = date.atTime(11, 0).atZone(MADRID).toInstant();

        assertThatThrownBy(() -> service.validateBookable(candidateStart, 60))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void validateBookable_succeedsAtExactlyFifteenMinutesAfterExistingBookingEnds() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 45-minute booking 10:00-10:45 — a candidate at 11:00 is exactly 15 minutes later.
        Instant existingStart = date.atTime(10, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 45)));
        Instant candidateStart = date.atTime(11, 0).atZone(MADRID).toInstant();

        assertThatCode(() -> service.validateBookable(candidateStart, 60)).doesNotThrowAnyException();
    }

    @Test
    void validateBookable_rejectsCandidateEndingWithinBufferBeforeExistingBookingStarts() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(availabilityRepo.findDayWindows(eq(date)))
                .thenReturn(List.of(dw(date, "09:00", "13:00")));
        // Existing 60-minute booking 11:00-12:00. A candidate 10:00-11:00 touches it with 0 min gap —
        // previously allowed (no overlap), now rejected (FR-002).
        Instant existingStart = date.atTime(11, 0).atZone(MADRID).toInstant();
        when(bookingRepo.findConfirmedBookingIntervalsBetween(any(), any()))
                .thenReturn(List.of(new BookingRepository.BookedInterval(existingStart, 60)));
        Instant candidateStart = date.atTime(10, 0).atZone(MADRID).toInstant();

        assertThatThrownBy(() -> service.validateBookable(candidateStart, 60))
                .isInstanceOf(SlotUnavailableException.class);
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

    @Test
    void slotInstantsShiftCorrectlyAcrossTheOctoberDstTransition() {
        // Spain's clocks go back one hour at 03:00 CEST on the last Sunday of October
        // (2026-10-25). A 09:00 local Madrid slot must be 07:00 UTC (CEST, UTC+2) the week
        // before the transition, and 08:00 UTC (CET, UTC+1) the week after — verifying the
        // existing ZoneId-based conversion (not new logic) handles the fold correctly, per FR-006.
        LocalDate weekBefore = LocalDate.of(2026, 10, 19); // Monday, still CEST
        LocalDate weekAfter = LocalDate.of(2026, 10, 26);  // Monday, already CET
        Clock dstClock = Clock.fixed(weekBefore.atStartOfDay(MADRID).toInstant(), MADRID);

        AvailabilityRepository repo = mock(AvailabilityRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.findConfirmedBookingIntervalsBetween(any(), any())).thenReturn(List.of());
        when(repo.findMaterializedWeekStarts(any(), any())).thenReturn(List.of(weekBefore, weekAfter));
        when(repo.findDayWindowsBetween(any(), any())).thenReturn(List.of(
                dw(weekBefore, "09:00", "10:00"),
                dw(weekAfter, "09:00", "10:00")));
        when(repo.findDayWindows(any())).thenReturn(List.of());

        AvailabilityService dstService = new AvailabilityService(new SchedulingProperties(), bookings, repo, dstClock);

        Instant beforeTransition = dstService.generateSchedule(60).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(weekBefore))
                .findFirst().orElseThrow().getStart();
        Instant afterTransition = dstService.generateSchedule(60).stream()
                .filter(s -> s.getStart().atZone(MADRID).toLocalDate().equals(weekAfter))
                .findFirst().orElseThrow().getStart();

        assertThat(beforeTransition).isEqualTo(Instant.parse("2026-10-19T07:00:00Z")); // CEST, UTC+2
        assertThat(afterTransition).isEqualTo(Instant.parse("2026-10-26T08:00:00Z"));  // CET, UTC+1
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
