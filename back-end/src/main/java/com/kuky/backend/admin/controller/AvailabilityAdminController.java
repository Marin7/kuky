package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.DayWindow;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Teacher-only availability management. Admin-gated by the /api/v1/admin/** matcher.
 * Manages the general weekly template ({@code weekly}) and the materialized per-day source of
 * truth ({@code days}). Times are "HH:mm" (teacher local), dates "YYYY-MM-DD".
 */
@RestController
@RequestMapping("/api/v1/admin/availability")
public class AvailabilityAdminController {

    private final AvailabilityRepository repository;
    private final AvailabilityService availabilityService;
    private final SchedulingProperties props;
    private final Clock clock;

    public AvailabilityAdminController(AvailabilityRepository repository,
                                       AvailabilityService availabilityService,
                                       SchedulingProperties props,
                                       Clock clock) {
        this.repository = repository;
        this.availabilityService = availabilityService;
        this.props = props;
        this.clock = clock;
    }

    @GetMapping
    public AvailabilityResponse getAvailability() {
        availabilityService.ensureCurrentHorizonMaterialized();

        List<WeeklyWindowDto> weekly = repository.findAllRules().stream()
                .map(r -> new WeeklyWindowDto(r.getId(), r.getDayOfWeek(),
                        r.getStartTime().toString(), r.getEndTime().toString()))
                .toList();

        LocalDate start = availabilityService.getHorizonStartDate();
        LocalDate end = availabilityService.getHorizonEndDate();
        Map<LocalDate, List<DayWindowDto>> byDate = repository.findDayWindowsBetween(start, end).stream()
                .collect(Collectors.groupingBy(DayWindow::date,
                        Collectors.mapping(w -> new DayWindowDto(w.startTime().toString(), w.endTime().toString()),
                                Collectors.toList())));

        List<DayAvailabilityDto> days = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            days.add(new DayAvailabilityDto(d.toString(), byDate.getOrDefault(d, List.of())));
        }
        return new AvailabilityResponse(weekly, days);
    }

    @PutMapping("/weekly")
    public UpdateWeeklyResponse updateWeekly(@Valid @RequestBody UpdateWeeklyRequest request) {
        List<AvailabilityRule> rules = request.windows().stream().map(w -> {
            LocalTime start = parseTime(w.startTime());
            LocalTime end = parseTime(w.endTime());
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio.");
            }
            return new AvailabilityRule(w.dayOfWeek(), start, end);
        }).toList();

        repository.replaceWeekly(rules);

        List<WeeklyWindowDto> saved = repository.findAllRules().stream()
                .map(r -> new WeeklyWindowDto(r.getId(), r.getDayOfWeek(),
                        r.getStartTime().toString(), r.getEndTime().toString()))
                .toList();
        return new UpdateWeeklyResponse(saved, conflicts());
    }

    @PutMapping("/days/{date}")
    public UpdateDayResponse updateDay(@PathVariable String date,
                                       @Valid @RequestBody UpdateDayRequest request) {
        LocalDate day = LocalDate.parse(date);
        if (day.isBefore(today())) {
            throw new IllegalArgumentException("La fecha no puede estar en el pasado.");
        }
        LocalDate horizonStart = availabilityService.getHorizonStartDate();
        if (day.isBefore(horizonStart) || !day.isBefore(availabilityService.getHorizonEndDate())) {
            throw new IllegalArgumentException("La fecha está fuera del rango editable.");
        }

        // Ensure the week is materialized (marker present) before overriding one of its dates,
        // otherwise a later materialization pass would re-seed it on top of the edit.
        availabilityService.ensureCurrentHorizonMaterialized();

        List<DayWindow> windows = request.windows().stream().map(w -> {
            LocalTime start = parseTime(w.startTime());
            LocalTime end = parseTime(w.endTime());
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio.");
            }
            return new DayWindow(day, start, end);
        }).toList();

        repository.replaceDayWindows(day, windows);

        List<DayWindowDto> saved = repository.findDayWindows(day).stream()
                .map(w -> new DayWindowDto(w.startTime().toString(), w.endTime().toString()))
                .toList();
        return new UpdateDayResponse(day.toString(), saved, conflicts());
    }

    private List<BookingConflictDto> conflicts() {
        return availabilityService.findConfirmedBookingsOutsideAvailability().stream()
                .map(b -> new BookingConflictDto(b.id(), b.email(), b.slotStart()))
                .toList();
    }

    private LocalTime parseTime(String hhmm) {
        try {
            return LocalTime.parse(hhmm);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Hora no válida: " + hhmm);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of(props.getScheduling().getTeacherTimezone())));
    }
}
