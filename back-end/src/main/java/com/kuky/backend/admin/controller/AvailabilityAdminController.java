package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.model.AvailabilityException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Teacher-only availability management. Admin-gated by the /api/v1/admin/** matcher.
 * Times are "HH:mm" (teacher local), dates "YYYY-MM-DD".
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
        List<WeeklyWindowDto> weekly = repository.findAllRules().stream()
                .map(r -> new WeeklyWindowDto(r.getId(), r.getDayOfWeek(),
                        r.getStartTime().toString(), r.getEndTime().toString()))
                .toList();
        List<ExceptionResponse> exceptions = repository.findUpcomingExceptions(today()).stream()
                .map(this::toExceptionResponse)
                .toList();
        return new AvailabilityResponse(weekly, exceptions);
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
        List<BookingConflictDto> conflicts = availabilityService
                .findConfirmedBookingsOutsideAvailability().stream()
                .map(b -> new BookingConflictDto(b.id(), b.email(), b.slotStart()))
                .toList();
        return new UpdateWeeklyResponse(saved, conflicts);
    }

    @PostMapping("/exceptions")
    public ResponseEntity<ExceptionResponse> addException(@Valid @RequestBody ExceptionRequest request) {
        LocalDate date = LocalDate.parse(request.date());
        if (date.isBefore(today())) {
            throw new IllegalArgumentException("La fecha no puede estar en el pasado.");
        }
        LocalTime start = parseTime(request.startTime());
        LocalTime end = parseTime(request.endTime());
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio.");
        }
        AvailabilityException e = new AvailabilityException();
        e.setDate(date);
        e.setKind(AvailabilityException.Kind.valueOf(request.kind()));
        e.setStartTime(start);
        e.setEndTime(end);
        e = repository.insertException(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(toExceptionResponse(e));
    }

    @DeleteMapping("/exceptions/{id}")
    public ResponseEntity<Void> deleteException(@PathVariable UUID id) {
        int deleted = repository.deleteException(id);
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private ExceptionResponse toExceptionResponse(AvailabilityException e) {
        return new ExceptionResponse(e.getId(), e.getDate().toString(), e.getKind().name(),
                e.getStartTime().toString(), e.getEndTime().toString());
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
