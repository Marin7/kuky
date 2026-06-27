package com.kuky.backend.scheduling.service;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.DayWindow;
import com.kuky.backend.scheduling.model.Slot;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates the public schedule from each week's materialized availability snapshot
 * ({@code week_availability}), the source of truth for bookings. Weeks are lazily snapshotted
 * from the general template ({@code availability_rules}) the first time they enter the horizon;
 * the existing safeguards still layer on top (confirmed bookings show BOOKED, past / within
 * lead-time slots show UNAVAILABLE).
 */
@Service
public class AvailabilityService {

    /** Rolling horizon length (weeks) for materialization, the public schedule, and bookings. */
    public static final int HORIZON_WEEKS = 4;

    private final SchedulingProperties props;
    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final Clock clock;

    public AvailabilityService(SchedulingProperties props,
                               BookingRepository bookingRepository,
                               AvailabilityRepository availabilityRepository,
                               Clock clock) {
        this.props = props;
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.clock = clock;
    }

    public List<Slot> generateSchedule() {
        ZoneId zone = zone();
        int durationMinutes = props.getScheduling().getClassDurationMinutes();
        int minLeadHours = props.getScheduling().getMinLeadHours();

        LocalDate horizonStart = horizonStartDate();
        LocalDate horizonEnd = horizonStart.plusWeeks(HORIZON_WEEKS);
        ensureWeeksMaterialized(horizonStart, horizonEnd);

        Instant horizonStartInstant = horizonStart.atStartOfDay(zone).toInstant();
        Instant horizonEndInstant = horizonEnd.atStartOfDay(zone).toInstant();

        Set<Instant> bookedStarts = bookingRepository
                .findConfirmedSlotStartsBetween(horizonStartInstant, horizonEndInstant)
                .stream().collect(Collectors.toSet());

        Map<LocalDate, List<Interval>> intervalsByDate =
                availabilityRepository.findDayWindowsBetween(horizonStart, horizonEnd).stream()
                        .collect(Collectors.groupingBy(DayWindow::date,
                                Collectors.mapping(w -> new Interval(w.startTime(), w.endTime()),
                                        Collectors.toList())));

        Instant now = clock.instant();
        Instant leadCutoff = now.plusSeconds((long) minLeadHours * 3600);

        List<Slot> slots = new ArrayList<>();
        LocalDate date = horizonStart;
        while (date.isBefore(horizonEnd)) {
            for (Interval window : merge(intervalsByDate.getOrDefault(date, List.of()))) {
                LocalTime time = window.start;
                while (!time.plusMinutes(durationMinutes).isAfter(window.end)) {
                    Instant start = date.atTime(time).atZone(zone).toInstant();
                    Instant end = start.plusSeconds((long) durationMinutes * 60);
                    Slot.Status status;
                    if (bookedStarts.contains(start)) {
                        status = Slot.Status.BOOKED;
                    } else if (!start.isAfter(leadCutoff) || !start.isAfter(now)) {
                        status = Slot.Status.UNAVAILABLE;
                    } else {
                        status = Slot.Status.OPEN;
                    }
                    slots.add(new Slot(start, end, status));
                    time = time.plusMinutes(durationMinutes);
                }
            }
            date = date.plusDays(1);
        }
        return slots;
    }

    public void validateBookable(Instant slotStart) {
        ZoneId zone = zone();
        int durationMinutes = props.getScheduling().getClassDurationMinutes();
        int minLeadHours = props.getScheduling().getMinLeadHours();

        LocalDate horizonStart = horizonStartDate();
        LocalDate horizonEnd = horizonStart.plusWeeks(HORIZON_WEEKS);
        ensureWeeksMaterialized(horizonStart, horizonEnd);

        // Must be at least minLeadHours in the future.
        Instant leadCutoff = clock.instant().plusSeconds((long) minLeadHours * 3600);
        if (!slotStart.isAfter(leadCutoff)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.LEAD);
        }

        // Must fall within the two-week horizon.
        Instant horizonStartInstant = horizonStart.atStartOfDay(zone).toInstant();
        Instant horizonEndInstant = horizonEnd.atStartOfDay(zone).toInstant();
        if (slotStart.isBefore(horizonStartInstant) || !slotStart.isBefore(horizonEndInstant)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.RANGE);
        }

        // Must align to a generated slot inside a saved window.
        ZonedDateTime zdt = slotStart.atZone(zone);
        LocalDate date = zdt.toLocalDate();
        LocalTime time = zdt.toLocalTime();
        if (time.getSecond() != 0 || time.getNano() != 0
                || time.getMinute() % durationMinutes != 0
                || !fitsAvailability(date, time, durationMinutes)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.RANGE);
        }
    }

    /** Upcoming confirmed bookings whose start no longer falls inside saved availability (FR-010). */
    public List<BookingRepository.ConfirmedBookingView> findConfirmedBookingsOutsideAvailability() {
        int durationMinutes = props.getScheduling().getClassDurationMinutes();
        LocalDate horizonStart = horizonStartDate();
        LocalDate horizonEnd = horizonStart.plusWeeks(HORIZON_WEEKS);
        ensureWeeksMaterialized(horizonStart, horizonEnd);

        List<BookingRepository.ConfirmedBookingView> upcoming =
                bookingRepository.findUpcomingConfirmedBookings(clock.instant());

        List<BookingRepository.ConfirmedBookingView> conflicts = new ArrayList<>();
        for (var b : upcoming) {
            ZonedDateTime zdt = b.slotStart().atZone(zone());
            if (!fitsAvailability(zdt.toLocalDate(), zdt.toLocalTime(), durationMinutes)) {
                conflicts.add(b);
            }
        }
        return conflicts;
    }

    public Instant getHorizonStart() {
        return horizonStartDate().atStartOfDay(zone()).toInstant();
    }

    public Instant getHorizonEnd() {
        return horizonStartDate().plusWeeks(HORIZON_WEEKS).atStartOfDay(zone()).toInstant();
    }

    /** First day (Monday) of the current bookable horizon, in the teacher's timezone. */
    public LocalDate getHorizonStartDate() {
        return horizonStartDate();
    }

    /** Exclusive end date of the current bookable horizon. */
    public LocalDate getHorizonEndDate() {
        return horizonStartDate().plusWeeks(HORIZON_WEEKS);
    }

    /** Snapshot any not-yet-materialized week in the current horizon from the template. */
    public void ensureCurrentHorizonMaterialized() {
        LocalDate start = horizonStartDate();
        ensureWeeksMaterialized(start, start.plusWeeks(HORIZON_WEEKS));
    }

    // --- materialization ------------------------------------------------------

    /**
     * Snapshot any week in {@code [horizonStart, horizonEnd)} not yet materialized, copying the
     * current general template into that week's dates. Template edits therefore only affect weeks
     * not yet materialized (FR-007); already-materialized weeks keep their snapshot (FR-005).
     */
    public void ensureWeeksMaterialized(LocalDate horizonStart, LocalDate horizonEnd) {
        Set<LocalDate> materialized = new HashSet<>(
                availabilityRepository.findMaterializedWeekStarts(horizonStart, horizonEnd));

        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate week = horizonStart; week.isBefore(horizonEnd); week = week.plusWeeks(1)) {
            if (!materialized.contains(week)) {
                missing.add(week);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        List<AvailabilityRule> rules = availabilityRepository.findAllRules();
        for (LocalDate week : missing) {
            availabilityRepository.materializeWeek(week, templateWindowsForWeek(week, rules));
        }
    }

    private List<DayWindow> templateWindowsForWeek(LocalDate weekStart, List<AvailabilityRule> rules) {
        List<DayWindow> windows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            int iso = date.getDayOfWeek().getValue();
            for (AvailabilityRule r : rules) {
                if (r.getDayOfWeek() == iso) {
                    windows.add(new DayWindow(date, r.getStartTime(), r.getEndTime()));
                }
            }
        }
        return windows;
    }

    // --- internals ------------------------------------------------------------

    private ZoneId zone() {
        return ZoneId.of(props.getScheduling().getTeacherTimezone());
    }

    private LocalDate horizonStartDate() {
        return LocalDate.now(clock.withZone(zone()))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean fitsAvailability(LocalDate date, LocalTime time, int durationMinutes) {
        LocalTime end = time.plusMinutes(durationMinutes);
        List<Interval> intervals = availabilityRepository.findDayWindows(date).stream()
                .map(w -> new Interval(w.startTime(), w.endTime()))
                .collect(Collectors.toList());
        for (Interval iv : merge(intervals)) {
            if (!time.isBefore(iv.start) && !end.isAfter(iv.end)) {
                return true;
            }
        }
        return false;
    }

    private static List<Interval> merge(List<Interval> intervals) {
        if (intervals.isEmpty()) return List.of();
        List<Interval> sorted = new ArrayList<>(intervals);
        sorted.sort((a, b) -> a.start.compareTo(b.start));
        List<Interval> out = new ArrayList<>();
        Interval cur = new Interval(sorted.get(0).start, sorted.get(0).end);
        for (int i = 1; i < sorted.size(); i++) {
            Interval nxt = sorted.get(i);
            if (!nxt.start.isAfter(cur.end)) {
                if (nxt.end.isAfter(cur.end)) cur = new Interval(cur.start, nxt.end);
            } else {
                out.add(cur);
                cur = new Interval(nxt.start, nxt.end);
            }
        }
        out.add(cur);
        return out;
    }

    private record Interval(LocalTime start, LocalTime end) {}
}
