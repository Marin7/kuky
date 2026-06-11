package com.kuky.backend.scheduling.service;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.model.AvailabilityException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.Slot;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates the public schedule as virtual slots derived from teacher-managed availability
 * (weekly rules ∪ OPEN exceptions − BLOCK exceptions), with the existing safeguards layered
 * on top: confirmed bookings show BOOKED, and past / within-lead-time slots show UNAVAILABLE.
 */
@Service
public class AvailabilityService {

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
        LocalDate horizonEnd = horizonStart.plusWeeks(2);

        Instant horizonStartInstant = horizonStart.atStartOfDay(zone).toInstant();
        Instant horizonEndInstant = horizonEnd.atStartOfDay(zone).toInstant();

        Set<Instant> bookedStarts = bookingRepository
                .findConfirmedSlotStartsBetween(horizonStartInstant, horizonEndInstant)
                .stream().collect(Collectors.toSet());

        List<AvailabilityRule> rules = availabilityRepository.findAllRules();
        List<AvailabilityException> exceptions =
                availabilityRepository.findExceptionsBetween(horizonStart, horizonEnd);

        Instant now = clock.instant();
        Instant leadCutoff = now.plusSeconds((long) minLeadHours * 3600);

        List<Slot> slots = new ArrayList<>();
        LocalDate date = horizonStart;
        while (date.isBefore(horizonEnd)) {
            for (Interval window : availableIntervals(date, rules, exceptions)) {
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

        // Must be at least minLeadHours in the future.
        Instant leadCutoff = clock.instant().plusSeconds((long) minLeadHours * 3600);
        if (!slotStart.isAfter(leadCutoff)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.LEAD);
        }

        // Must fall within the two-week horizon.
        LocalDate horizonStart = horizonStartDate();
        LocalDate horizonEnd = horizonStart.plusWeeks(2);
        Instant horizonStartInstant = horizonStart.atStartOfDay(zone).toInstant();
        Instant horizonEndInstant = horizonEnd.atStartOfDay(zone).toInstant();
        if (slotStart.isBefore(horizonStartInstant) || !slotStart.isBefore(horizonEndInstant)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.RANGE);
        }

        // Must align to a generated slot inside an available window.
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
        ZoneId zone = zone();
        int durationMinutes = props.getScheduling().getClassDurationMinutes();
        List<AvailabilityRule> rules = availabilityRepository.findAllRules();

        List<BookingRepository.ConfirmedBookingView> upcoming =
                bookingRepository.findUpcomingConfirmedBookings(clock.instant());

        List<BookingRepository.ConfirmedBookingView> conflicts = new ArrayList<>();
        for (var b : upcoming) {
            ZonedDateTime zdt = b.slotStart().atZone(zone);
            // Per-date exceptions only matter for that single date.
            List<AvailabilityException> dayExceptions = availabilityRepository
                    .findExceptionsBetween(zdt.toLocalDate(), zdt.toLocalDate().plusDays(1));
            if (!fitsAvailability(zdt.toLocalDate(), zdt.toLocalTime(), durationMinutes, rules, dayExceptions)) {
                conflicts.add(b);
            }
        }
        return conflicts;
    }

    public Instant getHorizonStart() {
        return horizonStartDate().atStartOfDay(zone()).toInstant();
    }

    public Instant getHorizonEnd() {
        return horizonStartDate().plusWeeks(2).atStartOfDay(zone()).toInstant();
    }

    // --- internals -----------------------------------------------------------

    private ZoneId zone() {
        return ZoneId.of(props.getScheduling().getTeacherTimezone());
    }

    private LocalDate horizonStartDate() {
        return LocalDate.now(clock.withZone(zone()))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean fitsAvailability(LocalDate date, LocalTime time, int durationMinutes) {
        return fitsAvailability(date, time, durationMinutes,
                availabilityRepository.findAllRules(),
                availabilityRepository.findExceptionsBetween(date, date.plusDays(1)));
    }

    private boolean fitsAvailability(LocalDate date, LocalTime time, int durationMinutes,
                                     List<AvailabilityRule> rules, List<AvailabilityException> exceptions) {
        LocalTime end = time.plusMinutes(durationMinutes);
        for (Interval iv : availableIntervals(date, rules, exceptions)) {
            if (!time.isBefore(iv.start) && !end.isAfter(iv.end)) {
                return true;
            }
        }
        return false;
    }

    /** Available time intervals for a date: merge(weekly rules + OPEN exceptions) − BLOCK exceptions. */
    private List<Interval> availableIntervals(LocalDate date,
                                              List<AvailabilityRule> rules,
                                              List<AvailabilityException> exceptions) {
        int iso = date.getDayOfWeek().getValue(); // 1=Mon … 7=Sun
        List<Interval> positives = new ArrayList<>();
        for (AvailabilityRule r : rules) {
            if (r.getDayOfWeek() == iso) {
                positives.add(new Interval(r.getStartTime(), r.getEndTime()));
            }
        }
        for (AvailabilityException e : exceptions) {
            if (e.getDate().equals(date) && e.getKind() == AvailabilityException.Kind.OPEN) {
                positives.add(new Interval(e.getStartTime(), e.getEndTime()));
            }
        }
        List<Interval> merged = merge(positives);

        List<Interval> blocks = new ArrayList<>();
        for (AvailabilityException e : exceptions) {
            if (e.getDate().equals(date) && e.getKind() == AvailabilityException.Kind.BLOCK) {
                blocks.add(new Interval(e.getStartTime(), e.getEndTime()));
            }
        }
        return subtract(merged, blocks);
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

    private static List<Interval> subtract(List<Interval> base, List<Interval> blocks) {
        List<Interval> result = new ArrayList<>(base);
        for (Interval block : blocks) {
            List<Interval> next = new ArrayList<>();
            for (Interval iv : result) {
                // No overlap.
                if (!block.start.isBefore(iv.end) || !block.end.isAfter(iv.start)) {
                    next.add(iv);
                    continue;
                }
                // Left remainder.
                if (block.start.isAfter(iv.start)) {
                    next.add(new Interval(iv.start, block.start));
                }
                // Right remainder.
                if (block.end.isBefore(iv.end)) {
                    next.add(new Interval(block.end, iv.end));
                }
            }
            result = next;
        }
        return result;
    }

    private record Interval(LocalTime start, LocalTime end) {}
}
