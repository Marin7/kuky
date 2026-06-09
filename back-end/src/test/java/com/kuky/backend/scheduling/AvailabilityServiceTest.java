package com.kuky.backend.scheduling;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.model.Slot;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        SchedulingProperties props = new SchedulingProperties();
        props.getScheduling().setTeacherTimezone("Europe/Madrid");
        props.getScheduling().setDayStart("09:00");
        props.getScheduling().setDayEnd("18:00");
        props.getScheduling().setClassDurationMinutes(60);
        props.getScheduling().setMinLeadHours(24);
        props.getScheduling().setCancelCutoffHours(24);
        service = new AvailabilityService(props, bookingRepository);
    }

    @Test
    void generateSchedule_returnsSlotsCoveringCurrentAndNextWeek() {
        when(bookingRepository.findConfirmedSlotStartsBetween(any(), any())).thenReturn(List.of());

        List<Slot> slots = service.generateSchedule();

        ZoneId zone = ZoneId.of("Europe/Madrid");
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusWeeks(2);

        // 14 days × 9 slots/day (09:00–18:00 at 60-min intervals) = 126 total
        assertThat(slots).hasSize(14 * 9);

        Instant firstSlot = weekStart.atTime(9, 0).atZone(zone).toInstant();
        assertThat(slots.get(0).getStart()).isEqualTo(firstSlot);

        Instant lastDayFirstSlot = weekEnd.minusDays(1).atTime(9, 0).atZone(zone).toInstant();
        assertThat(slots).anyMatch(s -> s.getStart().equals(lastDayFirstSlot));
    }

    @Test
    void generateSchedule_marksSlotBooked_whenConfirmedBookingExists() {
        ZoneId zone = ZoneId.of("Europe/Madrid");
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // A slot 10 days from start (within next week), well in the future
        LocalDate futureDate = weekStart.plusDays(10);
        Instant bookedStart = futureDate.atTime(10, 0).atZone(zone).toInstant();

        when(bookingRepository.findConfirmedSlotStartsBetween(any(), any())).thenReturn(List.of(bookedStart));

        List<Slot> slots = service.generateSchedule();

        Slot bookedSlot = slots.stream().filter(s -> s.getStart().equals(bookedStart)).findFirst().orElseThrow();
        assertThat(bookedSlot.getStatus()).isEqualTo(Slot.Status.BOOKED);
    }

    @Test
    void generateSchedule_marksSlotUnavailable_whenWithinLeadWindow() {
        when(bookingRepository.findConfirmedSlotStartsBetween(any(), any())).thenReturn(List.of());

        List<Slot> slots = service.generateSchedule();

        // All slots in the past or within 24h lead window must be UNAVAILABLE
        Instant leadCutoff = Instant.now().plusSeconds(24L * 3600);
        slots.stream()
                .filter(s -> !s.getStart().isAfter(leadCutoff))
                .forEach(s -> assertThat(s.getStatus()).isEqualTo(Slot.Status.UNAVAILABLE));
    }

    @Test
    void validateBookable_throwsLead_whenSlotWithin24Hours() {
        Instant tooSoon = Instant.now().plusSeconds(3600); // 1 hour from now
        assertThatThrownBy(() -> service.validateBookable(tooSoon))
                .isInstanceOf(BookingNotAllowedException.class)
                .extracting(e -> ((BookingNotAllowedException) e).getReason())
                .isEqualTo(BookingNotAllowedException.Reason.LEAD);
    }

    @Test
    void validateBookable_throwsRange_whenSlotOutsideHorizon() {
        ZoneId zone = ZoneId.of("Europe/Madrid");
        // 30 days out, definitely outside the 2-week horizon
        Instant farFuture = LocalDate.now(zone).plusDays(30).atTime(10, 0).atZone(zone).toInstant();
        assertThatThrownBy(() -> service.validateBookable(farFuture))
                .isInstanceOf(BookingNotAllowedException.class)
                .extracting(e -> ((BookingNotAllowedException) e).getReason())
                .isEqualTo(BookingNotAllowedException.Reason.RANGE);
    }
}
