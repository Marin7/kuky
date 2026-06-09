package com.kuky.backend.scheduling.service;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.model.Slot;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final SchedulingProperties props;
    private final BookingRepository bookingRepository;

    public AvailabilityService(SchedulingProperties props, BookingRepository bookingRepository) {
        this.props = props;
        this.bookingRepository = bookingRepository;
    }

    public List<Slot> generateSchedule() {
        ZoneId zone = ZoneId.of(props.getScheduling().getTeacherTimezone());
        LocalTime dayStart = LocalTime.parse(props.getScheduling().getDayStart());
        LocalTime dayEnd = LocalTime.parse(props.getScheduling().getDayEnd());
        LocalTime lunchStart = LocalTime.parse(props.getScheduling().getLunchBreakStart());
        LocalTime lunchEnd = LocalTime.parse(props.getScheduling().getLunchBreakEnd());
        int durationMinutes = props.getScheduling().getClassDurationMinutes();
        int minLeadHours = props.getScheduling().getMinLeadHours();

        LocalDate today = LocalDate.now(zone);
        // Monday of current week
        LocalDate horizonStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // Sunday of next week (end exclusive = Monday + 14 days)
        LocalDate horizonEnd = horizonStart.plusWeeks(2);

        Instant horizonStartInstant = horizonStart.atStartOfDay(zone).toInstant();
        Instant horizonEndInstant = horizonEnd.atStartOfDay(zone).toInstant();

        Set<Instant> bookedStarts = bookingRepository
                .findConfirmedSlotStartsBetween(horizonStartInstant, horizonEndInstant)
                .stream().collect(Collectors.toSet());

        Instant now = Instant.now();
        Instant leadCutoff = now.plusSeconds((long) minLeadHours * 3600);

        List<Slot> slots = new ArrayList<>();
        LocalDate date = horizonStart;
        while (date.isBefore(horizonEnd)) {
            LocalTime time = dayStart;
            while (!time.plusMinutes(durationMinutes).isAfter(dayEnd)) {
                Instant start = date.atTime(time).atZone(zone).toInstant();
                Instant end = start.plusSeconds((long) durationMinutes * 60);
                boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                boolean duringLunch = !time.isBefore(lunchStart) && time.isBefore(lunchEnd);
                Slot.Status status;
                if (bookedStarts.contains(start)) {
                    status = Slot.Status.BOOKED;
                } else if (isWeekend || duringLunch || !start.isAfter(leadCutoff) || !start.isAfter(now)) {
                    status = Slot.Status.UNAVAILABLE;
                } else {
                    status = Slot.Status.OPEN;
                }
                slots.add(new Slot(start, end, status));
                time = time.plusMinutes(durationMinutes);
            }
            date = date.plusDays(1);
        }
        return slots;
    }

    public void validateBookable(Instant slotStart) {
        ZoneId zone = ZoneId.of(props.getScheduling().getTeacherTimezone());
        LocalTime dayStart = LocalTime.parse(props.getScheduling().getDayStart());
        LocalTime dayEnd = LocalTime.parse(props.getScheduling().getDayEnd());
        LocalTime lunchStart = LocalTime.parse(props.getScheduling().getLunchBreakStart());
        LocalTime lunchEnd = LocalTime.parse(props.getScheduling().getLunchBreakEnd());
        int durationMinutes = props.getScheduling().getClassDurationMinutes();
        int minLeadHours = props.getScheduling().getMinLeadHours();

        // Must be at least minLeadHours in the future
        Instant leadCutoff = Instant.now().plusSeconds((long) minLeadHours * 3600);
        if (!slotStart.isAfter(leadCutoff)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.LEAD);
        }

        // Must fall within the two-week horizon
        LocalDate today = LocalDate.now(zone);
        LocalDate horizonStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate horizonEnd = horizonStart.plusWeeks(2);
        Instant horizonStartInstant = horizonStart.atStartOfDay(zone).toInstant();
        Instant horizonEndInstant = horizonEnd.atStartOfDay(zone).toInstant();
        if (slotStart.isBefore(horizonStartInstant) || !slotStart.isBefore(horizonEndInstant)) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.RANGE);
        }

        // Must fall on a weekday and align to a valid slot boundary, and not be in the lunch break
        ZonedDateTime zdt = slotStart.atZone(zone);
        boolean isWeekend = zdt.getDayOfWeek() == DayOfWeek.SATURDAY
                || zdt.getDayOfWeek() == DayOfWeek.SUNDAY;
        LocalTime time = zdt.toLocalTime();
        boolean duringLunch = !time.isBefore(lunchStart) && time.isBefore(lunchEnd);
        if (isWeekend || duringLunch || time.isBefore(dayStart) || !time.plusMinutes(durationMinutes).isAfter(dayStart)
                || time.plusMinutes(durationMinutes).isAfter(dayEnd)
                || time.getSecond() != 0 || time.getNano() != 0
                || time.getMinute() % durationMinutes != 0) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.RANGE);
        }
    }

    public Instant getHorizonStart() {
        ZoneId zone = ZoneId.of(props.getScheduling().getTeacherTimezone());
        LocalDate today = LocalDate.now(zone);
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(zone).toInstant();
    }

    public Instant getHorizonEnd() {
        ZoneId zone = ZoneId.of(props.getScheduling().getTeacherTimezone());
        LocalDate today = LocalDate.now(zone);
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(2).atStartOfDay(zone).toInstant();
    }
}
